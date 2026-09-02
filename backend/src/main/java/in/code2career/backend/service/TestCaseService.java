package in.code2career.backend.service;

import in.code2career.backend.dto.TestCaseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestCaseService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ProblemRepository problemRepository;

    public TestCase createTestCase(TestCaseDto dto) {
        Optional<Problem> problemOptional = problemRepository.findById(dto.getProblemId());

        if (problemOptional.isEmpty()) {
            throw new RuntimeException("Problem not found!");
        }

        TestCase testCase = new TestCase();
        testCase.setInputData(dto.getInputData());
        testCase.setExpectedOutput(dto.getExpectedOutput());
        testCase.setHidden(dto.isHidden());
        testCase.setProblem(problemOptional.get());

        return testCaseRepository.save(testCase);
    }

    public List<TestCase> getTestCasesByProblem(Long problemId) {
        return testCaseRepository.findByProblemId(problemId);
    }
}