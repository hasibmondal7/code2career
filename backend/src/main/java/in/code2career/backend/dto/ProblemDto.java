package in.code2career.backend.dto;

import lombok.Data;

@Data
public class ProblemDto {
    private String title;
    private String description;
    private String difficulty;
    private String constraints;
    private Long topicId; // Kon topic-er under-e problem-ta thakbe tar ID
}