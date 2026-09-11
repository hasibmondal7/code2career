package in.code2career.backend.dto;

import java.time.LocalDate;

public record ActivityResponseDto(
        LocalDate activityDate,
        Integer problemsSolved,
        Integer acceptedSubmissions,
        Integer xpEarned
) {
}
