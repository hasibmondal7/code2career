package in.code2career.backend.controller;

import in.code2career.backend.dto.ProblemDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "http://localhost:5173")
public class ProblemController {

    @Autowired
    private ProblemService problemService;

    @PostMapping
    public Problem createProblem(@RequestBody ProblemDto problemDto) {
        return problemService.createProblem(problemDto);
    }

    @GetMapping
    public List<Problem> getAllProblems() {
        return problemService.getAllProblems();
    }

    @GetMapping("/topic/{topicId}")
    public List<Problem> getProblemsByTopic(@PathVariable Long topicId) {
        return problemService.getProblemsByTopic(topicId);
    }
}