package in.code2career.backend.controller;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "http://localhost:5173")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @PostMapping
    public Submission submitCode(@RequestBody SubmissionDto dto) {
        return submissionService.submitCode(dto);
    }

    @GetMapping("/user/{userId}")
    public List<Submission> getUserSubmissions(@PathVariable Long userId) {
        return submissionService.getUserSubmissions(userId);
    }
}