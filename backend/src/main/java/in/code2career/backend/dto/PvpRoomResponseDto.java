package in.code2career.backend.dto;

import in.code2career.backend.enums.PvpRoomStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PvpRoomResponseDto(
        String inviteCode,
        String roomName,
        PvpRoomStatus status,
        Integer timeLimitMinutes,
        Integer maxPlayers,
        Integer stakeCoins,
        String language,
        boolean publicMatch,
        Long problemId,
        String problemTitle,
        String problemDifficulty,
        Long hostId,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endsAt,
        Long winnerId,
        Integer winnerXp,
        List<PvpRoomPlayerResponseDto> players
) {
}
