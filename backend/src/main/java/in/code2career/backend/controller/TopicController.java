package in.code2career.backend.controller;

import in.code2career.backend.dto.TopicDto;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "http://localhost:5173")
public class TopicController {

    @Autowired
    private TopicService topicService;

    // Notun topic add korar jonne
    @PostMapping
    public Topic createTopic(@RequestBody TopicDto topicDto) {
        return topicService.createTopic(topicDto);
    }

    // Shob topic dekhar jonne
    @GetMapping
    public List<Topic> getAllTopics() {
        return topicService.getAllTopics();
    }
}