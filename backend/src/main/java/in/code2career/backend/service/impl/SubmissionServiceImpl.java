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

    // Constructor Injection (Best Practice)
    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository,
            TestCaseRepository testCaseRepository,
            CodeEvaluationService codeEvaluationService
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeEvaluationService = codeEvaluationService;
    }

    @Override
    @Transactional
    public SubmissionResponseDto submitCode(SubmissionDto dto) {
        Problem problem = problemRepository.findById(dto.getProblemId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Problem", "id", dto.getProblemId())
                );

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmailIgnoreCase(email);
        if (currentUser == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }

        // 1. Mapped Entity toiri ebong Initial Save (PENDING status e)
        Submission submission = SubmissionMapper.mapToEntity(dto, problem, currentUser);
        submission.setStatus(SubmissionStatus.PENDING);
        submission = submissionRepository.save(submission);

        // 2. Evaluation Engine Pipeline Start
        List<TestCase> testCases = testCaseRepository.findByProblemId(problem.getId());
        SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED; // Default ধরি shob thik ache

        for (TestCase tc : testCases) {
            // Engine-e code ebong input pathano
            String engineOutput = codeEvaluationService.evaluateJavaCode(dto.getCode(), tc.getInputData());

            // Result Verify kora
            if (engineOutput.equals("COMPILATION_ERROR")) {
                finalStatus = SubmissionStatus.COMPILATION_ERROR;
                break;
            } else if (engineOutput.equals("TIME_LIMIT_EXCEEDED")) {
                finalStatus = SubmissionStatus.TIME_LIMIT_EXCEEDED;
                break;
            } else if (engineOutput.equals("RUNTIME_ERROR") || engineOutput.equals("SYSTEM_ERROR")) {
                finalStatus = SubmissionStatus.RUNTIME_ERROR;
                break;
            } else if (!engineOutput.equals(tc.getExpectedOutput().trim())) {
                finalStatus = SubmissionStatus.WRONG_ANSWER;
                break;
            }
        }

        // 3. Final Status Update kora
        submission.setStatus(finalStatus);
        submission = submissionRepository.save(submission);

        // 4. Response DTO return kora
        return SubmissionMapper.mapToResponseDto(submission);
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