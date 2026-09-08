package in.code2career.backend.service;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.dto.SubmissionResponseDto;

import java.util.List;

public interface SubmissionService {

    SubmissionResponseDto submitCode(SubmissionDto dto);

    List<SubmissionResponseDto> getUserSubmissions(Long userId);
}
