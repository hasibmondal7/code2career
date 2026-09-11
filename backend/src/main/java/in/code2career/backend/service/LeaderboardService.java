package in.code2career.backend.service;

import in.code2career.backend.dto.LeaderboardEntryDto;

import java.util.List;

public interface LeaderboardService {

    List<LeaderboardEntryDto> getTopUsers();
}
