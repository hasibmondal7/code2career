package in.code2career.backend.service.impl;

import in.code2career.backend.dto.TopicDto;
import in.code2career.backend.dto.TopicResponseDto;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;
import in.code2career.backend.exception.ConflictException;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.mapper.TopicMapper;
import in.code2career.backend.repository.TopicRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.TopicService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public TopicServiceImpl(TopicRepository topicRepository, UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TopicResponseDto createTopic(TopicDto topicDto) {
        String topicName = topicDto.getName().trim();
        if (topicRepository.existsByNameIgnoreCase(topicName)) {
            throw new ConflictException("A topic with this name already exists");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User adminUser = userRepository.findByEmailIgnoreCase(email);
        if (adminUser == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }

        Topic topic = TopicMapper.mapToEntity(topicDto, adminUser);
        return TopicMapper.mapToResponseDto(topicRepository.save(topic));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponseDto> getAllTopics() {
        return topicRepository.findAll()
                .stream()
                .map(TopicMapper::mapToResponseDto)
                .toList();
    }
}
