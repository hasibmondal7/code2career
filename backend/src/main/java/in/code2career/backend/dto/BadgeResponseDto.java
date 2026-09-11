package in.code2career.backend.dto;

import java.time.LocalDateTime;

public record BadgeResponseDto(
        String code,
        String name,
        String description,
        LocalDateTime awardedAt
) {
}
