package in.code2career.backend.service.impl;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.dto.SubmissionResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.SubmissionStatus;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.mapper.SubmissionMapper;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.CodeEvaluationService;
import in.code2career.backend.service.SubmissionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeEvaluationService codeEvaluationService;
    private final AsyncEvaluationRunner asyncEvaluationRunner;
    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository,
            TestCaseRepository testCaseRepository,
            CodeEvaluationService codeEvaluationService,
            AsyncEvaluationRunner asyncEvaluationRunner

    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeEvaluationService = codeEvaluationService;
        this.asyncEvaluationRunner = asyncEvaluationRunner;
    }

    @Override
    @Transactional
    public SubmissionResponseDto submitCode(SubmissionDto dto) {
        Problem problem = problemRepository.findById(dto.getProblemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", dto.getProblemId()));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmailIgnoreCase(email);
        if (currentUser == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }

        // 1. Initial Save (Synchronous)
        Submission submission = SubmissionMapper.mapToEntity(dto, problem, currentUser);
        submission.setStatus(SubmissionStatus.PENDING);
        submission = submissionRepository.save(submission);

        // 2. Trigger Background Evaluation
        asyncEvaluationRunner.runEvaluation(submission.getId(), dto.getCode(), problem.getId());

        // 3. Return immediate response (PENDING status)
        return SubmissionMapper.mapToResponseDto(submission);
    }

    @Async // This runs in a separate thread pool
    public void evaluateSubmissionAsync(Long submissionId, String code, Long problemId) {
        try {
            List<TestCase> testCases = testCaseRepository.findByProblemId(problemId);
            String engineResult = codeEvaluationService.evaluate(code, testCases);

            // Fetch submission again to avoid detached entity issues
            Submission submission = submissionRepository.findById(submissionId).orElseThrow();
            submission.setStatus(SubmissionStatus.valueOf(engineResult));
            submissionRepository.save(submission);
        } catch (Exception e) {
            Submission submission = submissionRepository.findById(submissionId).orElseThrow();
            submission.setStatus(SubmissionStatus.SYSTEM_ERROR);
            submissionRepository.save(submission);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponseDto> getUserSubmissions(Long userId) {
        return submissionRepository.findByUserId(userId)
                .stream()
                .map(SubmissionMapper::mapToResponseDto)
                .toList();
    }
}