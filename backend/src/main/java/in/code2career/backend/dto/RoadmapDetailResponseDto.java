package in.code2career.backend.dto;

import java.util.List;

public record RoadmapDetailResponseDto(
        Long id,
        String slug,
        String title,
        String description,
        String icon,
        String accent,
        Integer displayOrder,
        int totalSteps,
        int completedSteps,
        int progressPercent,
        List<RoadmapStepResponseDto> steps
) {
}
