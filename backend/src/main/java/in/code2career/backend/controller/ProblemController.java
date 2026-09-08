package in.code2career.backend.controller;

import in.code2career.backend.dto.ProblemDto;
import in.code2career.backend.dto.ProblemResponseDto;
import in.code2career.backend.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @PostMapping
    public ResponseEntity<ProblemResponseDto> createProblem(@Valid @RequestBody ProblemDto problemDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(problemService.createProblem(problemDto));
    }

    @GetMapping
    public List<ProblemResponseDto> getAllProblems() {
        return problemService.getAllProblems();
    }

    @GetMapping("/topic/{topicId}")
    public List<ProblemResponseDto> getProblemsByTopic(@PathVariable Long topicId) {
        return problemService.getProblemsByTopic(topicId);
    }
}
