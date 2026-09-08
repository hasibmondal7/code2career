package in.code2career.backend.mapper;

import in.code2career.backend.dto.SubmissionDto;
import in.code2career.backend.dto.SubmissionResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Submission;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.SubmissionStatus;

public final class SubmissionMapper {

    private SubmissionMapper() {
    }

    public static Submission mapToEntity(SubmissionDto dto, Problem problem, User user) {
        return Submission.builder()
                .code(dto.getCode())
                .language(dto.getLanguage())
                .status(SubmissionStatus.PENDING)
                .problem(problem)
                .user(user)
                .build();
    }

    public static SubmissionDto mapToDto(Submission submission) {
        SubmissionDto dto = new SubmissionDto();
        dto.setCode(submission.getCode());
        dto.setLanguage(submission.getLanguage());
        dto.setProblemId(submission.getProblem().getId());

        return dto;
    }

    public static SubmissionResponseDto mapToResponseDto(Submission submission) {
        return new SubmissionResponseDto(
                submission.getId(),
                submission.getCode(),
                submission.getLanguage(),
                submission.getStatus(),
                submission.getSubmittedAt(),
                submission.getProblem().getId(),
                submission.getUser().getId()
        );
    }
}
