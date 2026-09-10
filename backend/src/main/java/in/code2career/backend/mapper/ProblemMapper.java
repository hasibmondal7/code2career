package in.code2career.backend.mapper;

import in.code2career.backend.dto.ProblemDto;
import in.code2career.backend.dto.ProblemResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;

public final class ProblemMapper {

    private ProblemMapper() {
    }

    public static Problem mapToEntity(ProblemDto dto, Topic topic, User adminUser) {
        return Problem.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .difficulty(dto.getDifficulty())
                .constraints(dto.getConstraints())
                .xpReward(dto.getXpReward())
                .topic(topic)
                .addedBy(adminUser)
                .build();
    }

    public static ProblemDto mapToDto(Problem problem) {
        ProblemDto dto = new ProblemDto();
        dto.setTitle(problem.getTitle());
        dto.setDescription(problem.getDescription());
        dto.setDifficulty(problem.getDifficulty());
        dto.setConstraints(problem.getConstraints());
        dto.setXpReward(problem.getXpReward());

        if (problem.getTopic() != null) {
            dto.setTopicId(problem.getTopic().getId());
        }

        return dto;
    }

    public static ProblemResponseDto mapToResponseDto(Problem problem) {
        Long topicId = (problem.getTopic() != null) ? problem.getTopic().getId() : null;
        Long addedById = (problem.getAddedBy() != null) ? problem.getAddedBy().getId() : null;

        return new ProblemResponseDto(
                problem.getId(),
                problem.getTitle(),
                problem.getDescription(),
                problem.getDifficulty(),
                problem.getConstraints(),
                problem.getXpReward(),
                topicId,
                addedById
        );
    }
}