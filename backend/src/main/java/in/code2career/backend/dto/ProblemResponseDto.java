package in.code2career.backend.dto;

import in.code2career.backend.enums.Difficulty;

public record ProblemResponseDto(
        Long id,
        String title,
        String description,
        Difficulty difficulty,
        String constraints,
        Integer xpReward,
        Long topicId,
        Long addedByUserId
) {
}