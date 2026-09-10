package in.code2career.backend.dto;

import in.code2career.backend.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProblemDto {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    @Size(max = 10_000, message = "Constraints must not exceed 10000 characters")
    private String constraints;

    @NotNull(message = "XP Reward is required")
    @Positive(message = "XP Reward must be positive")
    private Integer xpReward;

    @NotNull(message = "Topic ID is required")
    @Positive(message = "Topic ID must be positive")
    private Long topicId;
}