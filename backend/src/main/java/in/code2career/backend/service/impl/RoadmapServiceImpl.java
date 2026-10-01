package in.code2career.backend.service.impl;

import in.code2career.backend.dto.RoadmapDetailResponseDto;
import in.code2career.backend.dto.RoadmapResponseDto;
import in.code2career.backend.dto.RoadmapStepResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Roadmap;
import in.code2career.backend.entity.RoadmapProgress;
import in.code2career.backend.entity.RoadmapStep;
import in.code2career.backend.entity.User;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.repository.RoadmapProgressRepository;
import in.code2career.backend.repository.RoadmapRepository;
import in.code2career.backend.repository.RoadmapStepRepository;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.RoadmapService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoadmapServiceImpl implements RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final RoadmapStepRepository roadmapStepRepository;
    private final RoadmapProgressRepository roadmapProgressRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;

    public RoadmapServiceImpl(
            RoadmapRepository roadmapRepository,
            RoadmapStepRepository roadmapStepRepository,
            RoadmapProgressRepository roadmapProgressRepository,
            UserRepository userRepository,
            ProblemRepository problemRepository
    ) {
        this.roadmapRepository = roadmapRepository;
        this.roadmapStepRepository = roadmapStepRepository;
        this.roadmapProgressRepository = roadmapProgressRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadmapResponseDto> getRoadmapsForCurrentUser() {
        User user = currentUser();
        Set<Long> completedStepIds = completedStepIds(user.getId());
        return roadmapRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(roadmap -> toResponse(roadmap, roadmapStepRepository.findByRoadmapIdOrderByStepOrderAsc(roadmap.getId()), completedStepIds))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoadmapDetailResponseDto getRoadmapForCurrentUser(Long roadmapId) {
        User user = currentUser();
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap", "id", roadmapId));
        Set<Long> completedStepIds = completedStepIds(user.getId());
        List<RoadmapStep> steps = roadmapStepRepository.findByRoadmapIdOrderByStepOrderAsc(roadmapId);
        RoadmapResponseDto summary = toResponse(roadmap, steps, completedStepIds);
        List<RoadmapStepResponseDto> stepResponses = steps.stream()
                .map(step -> toStepResponse(step, completedStepIds))
                .toList();
        return new RoadmapDetailResponseDto(
                summary.id(),
                summary.slug(),
                summary.title(),
                summary.description(),
                summary.icon(),
                summary.accent(),
                summary.displayOrder(),
                summary.totalSteps(),
                summary.completedSteps(),
                summary.progressPercent(),
                stepResponses
        );
    }

    @Override
    @Transactional
    public void recordAcceptedProblem(User user, Long problemId) {
        List<RoadmapStep> matchingSteps = roadmapStepRepository.findByProblemId(problemId);
        Set<Long> completedStepIds = completedStepIds(user.getId());

        if (matchingSteps.isEmpty()) {
            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", problemId));
            Long topicId = problem.getTopic() == null ? null : problem.getTopic().getId();
            matchingSteps = roadmapStepRepository.findAllByOrderByRoadmap_IdAscStepOrderAsc()
                    .stream()
                    .filter(step -> step.getProblem() == null
                            && step.getTopic() != null
                            && topicId != null
                            && topicId.equals(step.getTopic().getId()))
                    .toList();
        }

        Set<Long> roadmapIdsHandled = new HashSet<>();
        for (RoadmapStep step : matchingSteps) {
            if (completedStepIds.contains(step.getId())) {
                continue;
            }
            if (step.getProblem() == null && !roadmapIdsHandled.add(step.getRoadmap().getId())) {
                continue;
            }
            roadmapProgressRepository.insertIfAbsent(user.getId(), step.getId());
        }
    }

    private RoadmapResponseDto toResponse(Roadmap roadmap, List<RoadmapStep> steps, Set<Long> completedStepIds) {
        int completed = (int) steps.stream().filter(step -> completedStepIds.contains(step.getId())).count();
        int percent = steps.isEmpty() ? 0 : Math.round(completed * 100.0f / steps.size());
        return new RoadmapResponseDto(
                roadmap.getId(),
                roadmap.getSlug(),
                roadmap.getTitle(),
                roadmap.getDescription(),
                roadmap.getIcon(),
                roadmap.getAccent(),
                roadmap.getDisplayOrder(),
                steps.size(),
                completed,
                percent
        );
    }

    private RoadmapStepResponseDto toStepResponse(RoadmapStep step, Set<Long> completedStepIds) {
        return new RoadmapStepResponseDto(
                step.getId(),
                step.getStepOrder(),
                step.getTitle(),
                step.getDescription(),
                step.getXpReward(),
                step.getTopic() == null ? null : step.getTopic().getId(),
                step.getProblem() == null ? null : step.getProblem().getId(),
                completedStepIds.contains(step.getId())
        );
    }

    private Set<Long> completedStepIds(Long userId) {
        return roadmapProgressRepository.findByUserId(userId)
                .stream()
                .map(RoadmapProgress::getRoadmapStep)
                .map(RoadmapStep::getId)
                .collect(Collectors.toSet());
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }
        return user;
    }
}
