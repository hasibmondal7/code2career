package in.code2career.backend.mapper;

import in.code2career.backend.dto.TopicDto;
import in.code2career.backend.dto.TopicResponseDto;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;

public final class TopicMapper {

    private TopicMapper() {
    }

    public static Topic mapToEntity(TopicDto dto, User addedBy) {
        return Topic.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .addedBy(addedBy)
                .build();
    }

    public static TopicDto mapToDto(Topic topic) {
        TopicDto dto = new TopicDto();
        dto.setName(topic.getName());
        dto.setDescription(topic.getDescription());
        return dto;
    }

    public static TopicResponseDto mapToResponseDto(Topic topic) {
        Long addedById = null;
        if (topic.getAddedBy() != null) {
            addedById = topic.getAddedBy().getId();
        }

        return new TopicResponseDto(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                addedById
        );
    }
}