package in.code2career.backend.dto;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        Integer xp,
        Integer level,
        LocalDateTime createdAt
) {
}
