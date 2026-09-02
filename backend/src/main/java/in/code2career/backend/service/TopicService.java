package in.code2career.backend.service;

import in.code2career.backend.dto.TopicDto;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;
import in.code2career.backend.repository.TopicRepository;
import in.code2career.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicService {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository; // User khujar jonne eta add kora holo

    public Topic createTopic(TopicDto topicDto) {
        Topic topic = new Topic();
        topic.setName(topicDto.getName());
        topic.setDescription(topicDto.getDescription());

        // 1. JWT Token theke logged-in user-er email ber kora hochche
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Database theke ashol User object tule ana hochche
        User adminUser = userRepository.findByEmail(email);

        // 3. Topic-er sathe ei user-ke jure deya hochche
        topic.setAddedBy(adminUser);

        return topicRepository.save(topic);
    }

    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }
}