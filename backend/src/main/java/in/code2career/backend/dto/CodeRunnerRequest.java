package in.code2career.backend.dto;

import java.util.List;

public record CodeRunnerRequest(
        String code,
        String customInput,
        List<RunnerTestCase> testCases
) {
    public record RunnerTestCase(String inputData, String expectedOutput) {
    }
}
