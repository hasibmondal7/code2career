package in.code2career.backend.service;

import in.code2career.backend.dto.TestCaseDto;
import in.code2career.backend.dto.TestCaseResponseDto;

import java.util.List;

public interface TestCaseService {

    TestCaseResponseDto createTestCase(TestCaseDto dto);

    List<TestCaseResponseDto> getTestCasesByProblem(Long problemId);
}
