package in.code2career.backend.service;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.User;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private UserRepository userRepository;

    public Submission submitCode(SubmissionDto dto) {
        Optional<Problem> problemOpt = problemRepository.findById(dto.getProblemId());
        if (problemOpt.isEmpty()) {
            throw new RuntimeException("Problem not found!");
        }

        // Token theke current user ber kora
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email);

        Submission submission = new Submission();
        submission.setCode(dto.getCode());
        submission.setLanguage(dto.getLanguage());
        submission.setStatus("Pending"); // Pore evaluation engine eta update korbe
        submission.setProblem(problemOpt.get());
        submission.setUser(currentUser);

        return submissionRepository.save(submission);
    }

    public List<Submission> getUserSubmissions(Long userId) {
        return submissionRepository.findByUserId(userId);
    }
}