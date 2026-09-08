package in.code2career.backend.dto;

public record AuthResponseDto(
        String accessToken,
        String tokenType
) {
}
