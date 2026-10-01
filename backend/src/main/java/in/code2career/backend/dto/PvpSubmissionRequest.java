package in.code2career.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PvpSubmissionRequest(
        @NotBlank @Size(max = 100_000) String code,
        @NotBlank @Size(max = 30) String language
) {
}
