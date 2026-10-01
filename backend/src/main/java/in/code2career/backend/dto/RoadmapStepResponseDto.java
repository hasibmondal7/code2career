package in.code2career.backend.dto;

public record RoadmapStepResponseDto(
        Long id,
        Integer stepOrder,
        String title,
        String description,
        Integer xpReward,
        Long topicId,
        Long problemId,
        boolean completed
) {
}
