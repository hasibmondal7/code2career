package in.code2career.backend.controller;

import in.code2career.backend.dto.TestCaseDto;
import in.code2career.backend.dto.TestCaseResponseDto;
import in.code2career.backend.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping
    public ResponseEntity<TestCaseResponseDto> createTestCase(@Valid @RequestBody TestCaseDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testCaseService.createTestCase(dto));
    }

    @GetMapping("/problem/{problemId}")
    public List<TestCaseResponseDto> getTestCasesByProblem(@PathVariable Long problemId) {
        return testCaseService.getTestCasesByProblem(problemId);
    }
}
