package in.code2career.backend.service;

import in.code2career.backend.dto.TopicDto;
import in.code2career.backend.dto.TopicResponseDto;

import java.util.List;

public interface TopicService {

    TopicResponseDto createTopic(TopicDto topicDto);

    List<TopicResponseDto> getAllTopics();
}
