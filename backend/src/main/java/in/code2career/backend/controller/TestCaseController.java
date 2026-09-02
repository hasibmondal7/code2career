package in.code2career.backend.controller;

import in.code2career.backend.dto.TestCaseDto;
import in.code2career.backend.entity.TestCase;
import in.code2career.backend.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testcases")
@CrossOrigin(origins = "http://localhost:5173")
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @PostMapping
    public TestCase createTestCase(@RequestBody TestCaseDto dto) {
        return testCaseService.createTestCase(dto);
    }

    @GetMapping("/problem/{problemId}")
    public List<TestCase> getTestCasesByProblem(@PathVariable Long problemId) {
        return testCaseService.getTestCasesByProblem(problemId);
    }
}