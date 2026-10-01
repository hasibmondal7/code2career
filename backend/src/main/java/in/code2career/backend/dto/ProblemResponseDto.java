package in.code2career.backend.dto;

import in.code2career.backend.enums.Difficulty;
import java.util.Map;

public record ProblemResponseDto(
        Long id,
        String title,
        String description,
        Difficulty difficulty,
        String constraints,
        Integer xpReward,
        Long topicId,
        Long addedByUserId,
        Map<String, String> templates
) {
}
