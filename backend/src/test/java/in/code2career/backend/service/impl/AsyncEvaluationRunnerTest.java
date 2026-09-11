package in.code2career.backend.service.impl;

import in.code2career.backend.dto.EvaluationResult;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.SubmissionStatus;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.repository.UserActivityRepository;
import in.code2career.backend.service.CodeEvaluationService;
import in.code2career.backend.service.BadgeService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AsyncEvaluationRunnerTest {

    private final TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
    private final CodeEvaluationService codeEvaluationService = mock(CodeEvaluationService.class);
    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProblemRepository problemRepository = mock(ProblemRepository.class);
    private final UserActivityRepository userActivityRepository = mock(UserActivityRepository.class);
    private final BadgeService badgeService = mock(BadgeService.class);
    private final AsyncEvaluationRunner runner = new AsyncEvaluationRunner(
            testCaseRepository,
            codeEvaluationService,
            submissionRepository,
            messagingTemplate,
            userRepository,
            problemRepository,
            userActivityRepository,
            badgeService
    );

    @Test
    void doesNotAwardXpAgainWhenSubmissionWasAlreadyProcessed() {
        User user = User.builder().id(1L).xp(10).build();
        Problem problem = Problem.builder().id(2L).xpReward(10).build();
        Submission submission = Submission.builder()
                .id(3L)
                .status(SubmissionStatus.PENDING)
                .user(user)
                .problem(problem)
                .xpAwarded(true)
                .build();

        when(testCaseRepository.findByProblemId(2L)).thenReturn(List.of());
        when(codeEvaluationService.evaluate("class Solution {}", List.of()))
                .thenReturn(new EvaluationResult("ACCEPTED", 5L));
        when(submissionRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(submission));

        runner.runEvaluation(3L, "class Solution {}", 2L);

        assertEquals(10, user.getXp());
        verify(userRepository, never()).save(any(User.class));
        verify(messagingTemplate).convertAndSend("/topic/submissions/3", "ACCEPTED");
    }
}
