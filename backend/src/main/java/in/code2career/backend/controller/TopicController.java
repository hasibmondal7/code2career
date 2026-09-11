package in.code2career.backend.controller;

import in.code2career.backend.dto.TopicDto;
import in.code2career.backend.dto.TopicResponseDto;
import in.code2career.backend.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TopicResponseDto> createTopic(@Valid @RequestBody TopicDto topicDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicService.createTopic(topicDto));
    }

    @GetMapping
    public List<TopicResponseDto> getAllTopics() {
        return topicService.getAllTopics();
    }
}
