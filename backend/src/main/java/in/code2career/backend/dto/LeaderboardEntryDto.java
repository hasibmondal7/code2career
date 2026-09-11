package in.code2career.backend.dto;

public record LeaderboardEntryDto(
        String username,
        Integer totalXp,
        Integer level,
        Integer currentStreak
) {
}
