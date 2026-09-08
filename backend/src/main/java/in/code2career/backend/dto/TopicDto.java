package in.code2career.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopicDto {

    @NotBlank(message = "Topic name is required")
    @Size(max = 100, message = "Topic name must not exceed 100 characters")
    private String name;

    @Size(max = 1_000, message = "Description must not exceed 1000 characters")
    private String description;
}
