package in.code2career.backend.dto;

import lombok.Data;

@Data
public class SubmissionDto {
    private String code;
    private String language;
    private Long problemId;
}