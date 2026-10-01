package in.code2career.backend.service;

import in.code2career.backend.dto.CreatePvpRoomRequest;
import in.code2career.backend.dto.PvpRoomResponseDto;
import in.code2career.backend.dto.PvpSubmissionRequest;
import in.code2career.backend.dto.PvpSubmissionResponse;

import java.util.List;

public interface PvpRoomService {
    PvpRoomResponseDto createRoom(CreatePvpRoomRequest request);

    PvpRoomResponseDto joinRoom(String inviteCode);

    PvpRoomResponseDto getRoom(String inviteCode);

    PvpRoomResponseDto startRoom(String inviteCode);

    List<PvpRoomResponseDto> getOpenRooms();

    PvpSubmissionResponse submitCode(String inviteCode, PvpSubmissionRequest request);
}
