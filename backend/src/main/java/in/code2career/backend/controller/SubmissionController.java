package in.code2career.backend.controller;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.dto.SubmissionResponseDto;
import in.code2career.backend.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponseDto> submitCode(@Valid @RequestBody SubmissionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionService.submitCode(dto));
    }

    @GetMapping("/user/{userId}")
    public List<SubmissionResponseDto> getUserSubmissions(@PathVariable Long userId) {
        return submissionService.getUserSubmissions(userId);
    }
}
