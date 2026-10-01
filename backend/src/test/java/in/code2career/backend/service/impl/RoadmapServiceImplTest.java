package in.code2career.backend.service.impl;

import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.RoadmapProgress;
import in.code2career.backend.entity.RoadmapStep;
import in.code2career.backend.entity.User;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.RoadmapProgressRepository;
import in.code2career.backend.repository.RoadmapRepository;
import in.code2career.backend.repository.RoadmapStepRepository;
import in.code2career.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.*;

class RoadmapServiceImplTest {

    private final RoadmapRepository roadmapRepository = mock(RoadmapRepository.class);
    private final RoadmapStepRepository roadmapStepRepository = mock(RoadmapStepRepository.class);
    private final RoadmapProgressRepository roadmapProgressRepository = mock(RoadmapProgressRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProblemRepository problemRepository = mock(ProblemRepository.class);
    private final RoadmapServiceImpl roadmapService = new RoadmapServiceImpl(
            roadmapRepository,
            roadmapStepRepository,
            roadmapProgressRepository,
            userRepository,
            problemRepository
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotInsertProgressWhenStepIsAlreadyCompleted() {
        User user = User.builder().id(7L).email("user@example.com").build();
        RoadmapStep step = RoadmapStep.builder().id(12L).problem(Problem.builder().id(99L).build()).build();
        when(roadmapStepRepository.findByProblemId(99L)).thenReturn(List.of(step));
        when(roadmapProgressRepository.findByUserId(7L))
                .thenReturn(List.of(RoadmapProgress.builder().roadmapStep(step).build()));

        roadmapService.recordAcceptedProblem(user, 99L);

        verify(roadmapProgressRepository, never()).insertIfAbsent(7L, 12L);
    }

    @Test
    void insertsProgressForAnAcceptedProblemOnce() {
        User user = User.builder().id(7L).email("user@example.com").build();
        RoadmapStep step = RoadmapStep.builder().id(12L).problem(Problem.builder().id(99L).build()).build();
        when(roadmapStepRepository.findByProblemId(99L)).thenReturn(List.of(step));
        when(roadmapProgressRepository.findByUserId(7L)).thenReturn(List.of());
        when(roadmapProgressRepository.insertIfAbsent(7L, 12L)).thenReturn(1);

        roadmapService.recordAcceptedProblem(user, 99L);

        verify(roadmapProgressRepository).insertIfAbsent(7L, 12L);
    }
}
