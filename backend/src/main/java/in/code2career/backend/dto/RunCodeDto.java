package in.code2career.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RunCodeDto {
    private String language;
    @Size(max = 100_000, message = "Code must not exceed 100000 characters")
    private String code;
    @Size(max = 100_000, message = "Custom input must not exceed 100000 characters")
    private String customInput;
}