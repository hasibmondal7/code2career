package in.code2career.backend.dto;

public record PvpSubmissionResponse(
        String status,
        Long executionTimeMs,
        boolean winner,
        Integer coinsWon,
        Integer xpWon
) {
}
