package in.code2career.backend.service;

import in.code2career.backend.dto.ProblemDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.TopicRepository;
import in.code2career.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProblemService {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository;

    public Problem createProblem(ProblemDto problemDto) {
        Optional<Topic> topicOptional = topicRepository.findById(problemDto.getTopicId());

        if (topicOptional.isEmpty()) {
            throw new RuntimeException("Topic not found!");
        }

        Problem problem = new Problem();
        problem.setTitle(problemDto.getTitle());
        problem.setDescription(problemDto.getDescription());
        problem.setDifficulty(problemDto.getDifficulty());
        problem.setConstraints(problemDto.getConstraints());
        problem.setTopic(topicOptional.get());

        // JWT token theke admin-ke set kora hochche
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User adminUser = userRepository.findByEmail(email);
        problem.setAddedBy(adminUser);

        return problemRepository.save(problem);
    }

    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    public List<Problem> getProblemsByTopic(Long topicId) {
        return problemRepository.findByTopicId(topicId);
    }
}