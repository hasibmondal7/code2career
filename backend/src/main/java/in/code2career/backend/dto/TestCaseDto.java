package in.code2career.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TestCaseDto {

    @NotBlank(message = "Input data is required")
    private String inputData;

    @NotBlank(message = "Expected output is required")
    private String expectedOutput;

    private boolean isHidden;

    @NotNull(message = "Problem ID is required")
    @Positive(message = "Problem ID must be positive")
    private Long problemId;
}
