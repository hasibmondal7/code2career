package in.code2career.backend.mapper;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.dto.SubmissionResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.SubmissionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubmissionMapperTest {

    @Test
    void assignsPendingStatusToNewSubmission() {
        SubmissionDto request = new SubmissionDto();
        request.setCode("class Solution {}");
        request.setLanguage("java");
        request.setProblemId(10L);

        Problem problem = Problem.builder().id(10L).build();
        User user = User.builder().id(20L).build();

        Submission submission = SubmissionMapper.mapToEntity(request, problem, user);
        submission.setId(30L);

        SubmissionResponseDto response = SubmissionMapper.mapToResponseDto(submission);

        assertEquals(SubmissionStatus.PENDING, submission.getStatus());
        assertEquals(30L, response.id());
        assertEquals(10L, response.problemId());
        assertEquals(20L, response.userId());
    }
}
