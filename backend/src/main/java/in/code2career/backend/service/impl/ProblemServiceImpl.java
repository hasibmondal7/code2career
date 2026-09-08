package in.code2career.backend.service.impl;

import in.code2career.backend.dto.ProblemDto;
import in.code2career.backend.dto.ProblemResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.mapper.ProblemMapper;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.TopicRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.ProblemService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;

    private final TopicRepository topicRepository;

    private final UserRepository userRepository;

    public ProblemServiceImpl(
            ProblemRepository problemRepository,
            TopicRepository topicRepository,
            UserRepository userRepository
    ) {
        this.problemRepository = problemRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ProblemResponseDto createProblem(ProblemDto problemDto) {
        Topic topic = topicRepository.findById(problemDto.getTopicId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Topic", "id", problemDto.getTopicId())
                );

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User adminUser = userRepository.findByEmailIgnoreCase(email);
        if (adminUser == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }

        Problem problem = ProblemMapper.mapToEntity(problemDto, topic, adminUser);
        return ProblemMapper.mapToResponseDto(problemRepository.save(problem));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProblemResponseDto> getAllProblems() {
        return problemRepository.findAll()
                .stream()
                .map(ProblemMapper::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProblemResponseDto> getProblemsByTopic(Long topicId) {
        return problemRepository.findByTopicId(topicId)
                .stream()
                .map(ProblemMapper::mapToResponseDto)
                .toList();
    }
}
