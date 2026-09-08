package in.code2career.backend.dto;

import in.code2career.backend.enums.SubmissionStatus;

import java.time.LocalDateTime;

public record SubmissionResponseDto(
        Long id,
        String code,
        String language,
        SubmissionStatus status,
        LocalDateTime submittedAt,
        Long problemId,
        Long userId
) {
}
