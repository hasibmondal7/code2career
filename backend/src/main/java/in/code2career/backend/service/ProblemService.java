package in.code2career.backend.service;

import in.code2career.backend.dto.ProblemDto;
import in.code2career.backend.dto.ProblemResponseDto;

import java.util.List;

public interface ProblemService {

    ProblemResponseDto createProblem(ProblemDto problemDto);

    List<ProblemResponseDto> getAllProblems();

    List<ProblemResponseDto> getProblemsByTopic(Long topicId);
}
