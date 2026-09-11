package in.code2career.backend.dto;

public record CodeRunnerResponse(
        String status,
        Long executionTimeMs,
        String output,
        String error
) {
}
