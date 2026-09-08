package in.code2career.backend.dto;

public record TestCaseResponseDto(
        Long id,
        String inputData,
        String expectedOutput,
        boolean hidden,
        Long problemId
) {
}
