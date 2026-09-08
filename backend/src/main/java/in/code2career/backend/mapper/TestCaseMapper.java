package in.code2career.backend.mapper;

import in.code2career.backend.dto.TestCaseDto;
import in.code2career.backend.dto.TestCaseResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.TestCase;

public final class TestCaseMapper {

    private TestCaseMapper() {
    }

    public static TestCase mapToEntity(TestCaseDto dto, Problem problem) {
        return TestCase.builder()
                .inputData(dto.getInputData())
                .expectedOutput(dto.getExpectedOutput())
                .isHidden(dto.isHidden())
                .problem(problem)
                .build();
    }

    public static TestCaseResponseDto mapToResponseDto(TestCase testCase) {
        return new TestCaseResponseDto(
                testCase.getId(),
                testCase.getInputData(),
                testCase.getExpectedOutput(),
                testCase.isHidden(),
                testCase.getProblem().getId()
        );
    }
}
