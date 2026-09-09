package in.code2career.backend.service.impl;

import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.enums.SubmissionStatus;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.service.CodeEvaluationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AsyncEvaluationRunner {

    private final TestCaseRepository testCaseRepository;
    private final CodeEvaluationService codeEvaluationService;
    private final SubmissionRepository submissionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AsyncEvaluationRunner(TestCaseRepository testCaseRepository,
                                 CodeEvaluationService codeEvaluationService,
                                 SubmissionRepository submissionRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.testCaseRepository = testCaseRepository;
        this.codeEvaluationService = codeEvaluationService;
        this.submissionRepository = submissionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    public void runEvaluation(Long submissionId, String code, Long problemId) {
        try {
            // 1. Fetch test cases for the problem
            List<TestCase> testCases = testCaseRepository.findByProblemId(problemId);

            // 2. Evaluate code using Docker Engine
            String engineResult = codeEvaluationService.evaluate(code, testCases);

            // 3. Update database with final result
            Submission submission = submissionRepository.findById(submissionId).orElseThrow();
            submission.setStatus(SubmissionStatus.valueOf(engineResult));
            submissionRepository.save(submission);

            // 4. Push Live Status to Frontend via WebSocket
            messagingTemplate.convertAndSend("/topic/submissions/" + submissionId, engineResult);

        } catch (Exception e) {
            e.printStackTrace();

            // Handle exceptions and update database
            Submission submission = submissionRepository.findById(submissionId).orElseThrow();
            submission.setStatus(SubmissionStatus.SYSTEM_ERROR);
            submissionRepository.save(submission);

            // Push SYSTEM_ERROR to Frontend
            messagingTemplate.convertAndSend("/topic/submissions/" + submissionId, "SYSTEM_ERROR");
        }
    }
}