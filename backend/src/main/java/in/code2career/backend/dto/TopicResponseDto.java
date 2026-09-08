package in.code2career.backend.dto;

public record TopicResponseDto(
        Long id,
        String name,
        String description,
        Long addedByUserId
) {
}
