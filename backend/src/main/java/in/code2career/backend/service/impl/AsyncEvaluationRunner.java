package in.code2career.backend.service.impl;

import in.code2career.backend.dto.EvaluationResult;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.entity.User;
import in.code2career.backend.entity.UserActivity;
import in.code2career.backend.enums.SubmissionStatus;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.repository.UserActivityRepository;
import in.code2career.backend.service.CodeEvaluationService;
import in.code2career.backend.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEvaluationRunner {

    private final TestCaseRepository testCaseRepository;
    private final CodeEvaluationService codeEvaluationService;
    private final SubmissionRepository submissionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Injected new repositories for Gamification
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final UserActivityRepository userActivityRepository;
    private final BadgeService badgeService;

    @Async
    @Transactional
    public void runEvaluation(Long submissionId, String code, Long problemId) {
        try {
            // 1. Fetch test cases for the problem
            List<TestCase> testCases = testCaseRepository.findByProblemId(problemId);

            // 2. Evaluate code using Docker Engine (Returns EvaluationResult object)
            EvaluationResult evaluationResult = codeEvaluationService.evaluate(code, testCases);

            // 3. Update database with final status AND execution time
            Submission submission = submissionRepository.findByIdForUpdate(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found with ID: " + submissionId));

            String finalStatus = evaluationResult.getStatus();
            submission.setStatus(SubmissionStatus.valueOf(finalStatus));
            submission.setExecutionTimeMs(evaluationResult.getExecutionTimeMs());
            submissionRepository.save(submission);

            // 4. Gamification Logic: Update XP and Streak if ACCEPTED
            if ("ACCEPTED".equals(finalStatus) && !Boolean.TRUE.equals(submission.getXpAwarded())) {

                // Fixed: Fetched user directly from submission object
                User user = submission.getUser();
                if (user == null) {
                    throw new RuntimeException("User not found for this submission");
                }

                Problem problem = problemRepository.findById(problemId)
                        .orElseThrow(() -> new RuntimeException("Problem not found with ID: " + problemId));

                // Add XP (Level is calculated automatically in addXp method)
                user.addXp(problem.getXpReward());

                // Streak Calculation Logic
                LocalDate today = LocalDate.now();
                LocalDate lastActive = user.getLastActiveDate();

                if (lastActive == null || lastActive.isBefore(today)) {
                    if (lastActive != null && lastActive.plusDays(1).equals(today)) {
                        // Coded yesterday, increase streak
                        user.setCurrentStreak(user.getCurrentStreak() + 1);
                    } else {
                        // Streak broken or first time coding
                        user.setCurrentStreak(1);
                    }
                    user.setLastActiveDate(today);
                }

                userRepository.save(user);
                UserActivity activity = userActivityRepository
                        .findByUserIdAndActivityDate(user.getId(), today)
                        .orElseGet(() -> UserActivity.builder()
                                .user(user)
                                .activityDate(today)
                                .problemsSolved(0)
                                .acceptedSubmissions(0)
                                .xpEarned(0)
                                .build());
                activity.setProblemsSolved(activity.getProblemsSolved() + 1);
                activity.setAcceptedSubmissions(activity.getAcceptedSubmissions() + 1);
                activity.setXpEarned(activity.getXpEarned() + problem.getXpReward());
                userActivityRepository.save(activity);
                badgeService.awardEligibleBadges(user);
                submission.setXpAwarded(true);
                submissionRepository.save(submission);
                log.info("Gamification updated for User ID {}: XP = {}, Level = {}, Streak = {}",
                        user.getId(), user.getXp(), user.getLevel(), user.getCurrentStreak());
            }

            // 5. Push Live Status to Frontend via WebSocket
            messagingTemplate.convertAndSend("/topic/submissions/" + submissionId, finalStatus);

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