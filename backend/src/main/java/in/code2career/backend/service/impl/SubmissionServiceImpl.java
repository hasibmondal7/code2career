package in.code2career.backend.service.impl;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.dto.SubmissionResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.User;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.mapper.SubmissionMapper;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.SubmissionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;

    private final ProblemRepository problemRepository;

    private final UserRepository userRepository;

    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SubmissionResponseDto submitCode(SubmissionDto dto) {
        Problem problem = problemRepository.findById(dto.getProblemId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Problem", "id", dto.getProblemId())
                );

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmailIgnoreCase(email);
        if (currentUser == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }

        Submission submission = SubmissionMapper.mapToEntity(dto, problem, currentUser);
        return SubmissionMapper.mapToResponseDto(submissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponseDto> getUserSubmissions(Long userId) {
        return submissionRepository.findByUserId(userId)
                .stream()
                .map(SubmissionMapper::mapToResponseDto)
                .toList();
    }
}
