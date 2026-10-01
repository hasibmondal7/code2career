package in.code2career.backend.controller;

import in.code2career.backend.dto.CreatePvpRoomRequest;
import in.code2career.backend.dto.PvpRoomResponseDto;
import in.code2career.backend.dto.PvpSubmissionRequest;
import in.code2career.backend.dto.PvpSubmissionResponse;
import in.code2career.backend.service.PvpRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pvp/rooms")
@RequiredArgsConstructor
public class PvpRoomController {
    private final PvpRoomService pvpRoomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PvpRoomResponseDto createRoom(@Valid @RequestBody CreatePvpRoomRequest request) {
        return pvpRoomService.createRoom(request);
    }

    @GetMapping("/open")
    public java.util.List<PvpRoomResponseDto> getOpenRooms() {
        return pvpRoomService.getOpenRooms();
    }

    @PostMapping("/{inviteCode}/join")
    public PvpRoomResponseDto joinRoom(@PathVariable String inviteCode) {
        return pvpRoomService.joinRoom(inviteCode);
    }

    @GetMapping("/{inviteCode}")
    public PvpRoomResponseDto getRoom(@PathVariable String inviteCode) {
        return pvpRoomService.getRoom(inviteCode);
    }

    @PostMapping("/{inviteCode}/start")
    public PvpRoomResponseDto startRoom(@PathVariable String inviteCode) {
        return pvpRoomService.startRoom(inviteCode);
    }

    @PostMapping("/{inviteCode}/submit")
    public PvpSubmissionResponse submitCode(
            @PathVariable String inviteCode,
            @Valid @RequestBody PvpSubmissionRequest request
    ) {
        return pvpRoomService.submitCode(inviteCode, request);
    }
}
