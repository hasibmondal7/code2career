package in.code2career.backend.service.impl;

import in.code2career.backend.dto.EvaluationResult;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.enums.SubmissionStatus;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.service.CodeEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEvaluationRunner {

    private final TestCaseRepository testCaseRepository;
    private final CodeEvaluationService codeEvaluationService;
    private final SubmissionRepository submissionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void runEvaluation(Long submissionId, String code, Long problemId) {
        try {
            // 1. Fetch test cases for the problem
            List<TestCase> testCases = testCaseRepository.findByProblemId(problemId);

            // 2. Evaluate code using Docker Engine (Returns EvaluationResult object)
            EvaluationResult evaluationResult = codeEvaluationService.evaluate(code, testCases);

            // 3. Update database with final status AND execution time
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found with ID: " + submissionId));

            submission.setStatus(SubmissionStatus.valueOf(evaluationResult.getStatus()));
            submission.setExecutionTimeMs(evaluationResult.getExecutionTimeMs());
            submissionRepository.save(submission);

            // 4. Push Live Status to Frontend via WebSocket
            messagingTemplate.convertAndSend("/topic/submissions/" + submissionId, evaluationResult.getStatus());

        } catch (Exception e) {
            log.error("Error during evaluation for submission {}: {}", submissionId, e.getMessage(), e);

            // Handle exceptions and update database if submission exists
            submissionRepository.findById(submissionId).ifPresent(submission -> {
                submission.setStatus(SubmissionStatus.SYSTEM_ERROR);
                submission.setExecutionTimeMs(0L);
                submissionRepository.save(submission);
            });

            // Push SYSTEM_ERROR to Frontend
            messagingTemplate.convertAndSend("/topic/submissions/" + submissionId, "SYSTEM_ERROR");
        }
    }
}