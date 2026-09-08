package in.code2career.backend.service.impl;

import in.code2career.backend.dto.TestCaseDto;
import in.code2career.backend.dto.TestCaseResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.mapper.TestCaseMapper;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.service.TestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    public TestCaseServiceImpl(
            TestCaseRepository testCaseRepository,
            ProblemRepository problemRepository
    ) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public TestCaseResponseDto createTestCase(TestCaseDto dto) {
        Problem problem = problemRepository.findById(dto.getProblemId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Problem", "id", dto.getProblemId())
                );

        TestCase testCase = TestCaseMapper.mapToEntity(dto, problem);
        return TestCaseMapper.mapToResponseDto(testCaseRepository.save(testCase));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCaseResponseDto> getTestCasesByProblem(Long problemId) {
        return testCaseRepository.findByProblemId(problemId)
                .stream()
                .map(TestCaseMapper::mapToResponseDto)
                .toList();
    }
}
