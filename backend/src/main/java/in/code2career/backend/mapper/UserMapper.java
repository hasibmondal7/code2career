package in.code2career.backend.mapper;

import in.code2career.backend.dto.UserDto;
import in.code2career.backend.dto.UserResponseDto;
import in.code2career.backend.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static User mapToEntity(UserDto dto, String encodedPassword) {
        return User.builder()
                .username(dto.getUsername().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .password(encodedPassword)
                .xp(0)
                .level(1)
                .currentStreak(0)
                .build();
    }

    public static UserResponseDto mapToResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getXp(),
                user.getLevel(),
                user.getCurrentStreak(),
                user.getLastActiveDate(),
                user.getCreatedAt()
        );
    }
}