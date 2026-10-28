package in.code2career.backend.dto;

import in.code2career.backend.enums.UserRole;

import java.time.LocalDateTime;

public record AdminUserResponseDto(
        Long id,
        String username,
        String email,
        UserRole role,
        boolean active,
        Integer xp,
        Integer level,
        Integer currentStreak,
        LocalDateTime createdAt
) {
}
