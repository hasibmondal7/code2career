package in.code2career.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmissionDto {

    @NotBlank(message = "Code is required")
    @Size(max = 100_000, message = "Code must not exceed 100000 characters")
    private String code;

    @NotBlank(message = "Language is required")
    @Size(max = 30, message = "Language must not exceed 30 characters")
    private String language;

    @NotNull(message = "Problem ID is required")
    @Positive(message = "Problem ID must be positive")
    private Long problemId;
}