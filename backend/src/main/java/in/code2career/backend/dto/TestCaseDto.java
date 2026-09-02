package in.code2career.backend.dto;

import lombok.Data;

@Data
public class TestCaseDto {
    private String inputData;
    private String expectedOutput;
    private boolean isHidden;
    private Long problemId; // Kon problem-er test case seta bojhanor jonne
}