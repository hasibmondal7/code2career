package in.code2career.backend.dto;

public record RoadmapResponseDto(
        Long id,
        String slug,
        String title,
        String description,
        String icon,
        String accent,
        Integer displayOrder,
        int totalSteps,
        int completedSteps,
        int progressPercent
) {
}
