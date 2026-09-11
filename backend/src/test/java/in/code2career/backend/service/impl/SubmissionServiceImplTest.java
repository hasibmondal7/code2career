package in.code2career.backend.service.impl;

import in.code2career.backend.entity.User;
import in.code2career.backend.exception.ForbiddenException;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SubmissionServiceImplTest {

    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final ProblemRepository problemRepository = mock(ProblemRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AsyncEvaluationRunner asyncEvaluationRunner = mock(AsyncEvaluationRunner.class);
    private final SubmissionServiceImpl submissionService = new SubmissionServiceImpl(
            submissionRepository,
            problemRepository,
            userRepository,
            asyncEvaluationRunner
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsSubmissionsForAnotherUser() {
        User currentUser = User.builder().id(10L).email("hasib@example.com").build();
        when(userRepository.findByEmailIgnoreCase("hasib@example.com")).thenReturn(currentUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hasib@example.com", null)
        );

        assertThrows(ForbiddenException.class, () -> submissionService.getUserSubmissions(20L));

        verify(submissionRepository, never()).findByUserId(anyLong());
    }
}
