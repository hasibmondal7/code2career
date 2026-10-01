package in.code2career.backend.dto;

import java.time.LocalDateTime;

public record PvpRoomPlayerResponseDto(
        Long userId,
        String username,
        String profilePhoto,
        boolean host,
        LocalDateTime joinedAt,
        Integer coins
) {
}
