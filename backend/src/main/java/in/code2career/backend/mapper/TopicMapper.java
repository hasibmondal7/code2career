package in.code2career.backend.mapper;

import in.code2career.backend.dto.TopicDto;
import in.code2career.backend.dto.TopicResponseDto;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;

public final class TopicMapper {

    private TopicMapper() {
    }

    public static Topic mapToEntity(TopicDto dto, User adminUser) {
        return Topic.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .addedBy(adminUser)
                .build();
    }

    public static TopicResponseDto mapToResponseDto(Topic topic) {
        return new TopicResponseDto(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getAddedBy().getId()
        );
    }
}
