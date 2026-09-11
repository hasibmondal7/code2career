package in.code2career.backend.controller;

import in.code2career.backend.dto.RunCodeDto;
import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.dto.SubmissionResponseDto;
import in.code2career.backend.service.CodeEvaluationService;
import in.code2career.backend.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CodeEvaluationService codeEvaluationService;

    @PostMapping
    public ResponseEntity<SubmissionResponseDto> submitCode(@Valid @RequestBody SubmissionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionService.submitCode(dto));
    }

    @GetMapping("/user/{userId}")
    public List<SubmissionResponseDto> getUserSubmissions(@PathVariable Long userId) {
        return submissionService.getUserSubmissions(userId);
    }

    @GetMapping("/{submissionId}")
    public SubmissionResponseDto getSubmission(@PathVariable Long submissionId) {
        return submissionService.getSubmission(submissionId);
    }

    @PostMapping("/run")
    public ResponseEntity<String> runCustomCode(@Valid @RequestBody RunCodeDto runCodeDto) {
        String result = codeEvaluationService.executeCustomInput(
                runCodeDto.getCode(),
                runCodeDto.getCustomInput()
        );
        return ResponseEntity.ok(result);
    }
}