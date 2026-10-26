package in.code2career.backend.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RunCodeDto {
    @NotBlank(message = "Language is required")
    @Size(max = 30, message = "Language must not exceed 30 characters")
    private String language;

    @NotBlank(message = "Code is required")
    @Size(max = 100_000, message = "Code must not exceed 100000 characters")
    private String code;
    @Size(max = 100_000, message = "Custom input must not exceed 100000 characters")
    private String customInput;
}