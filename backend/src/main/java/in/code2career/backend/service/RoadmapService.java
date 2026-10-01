package in.code2career.backend.service;

import in.code2career.backend.dto.RoadmapDetailResponseDto;
import in.code2career.backend.dto.RoadmapResponseDto;
import in.code2career.backend.entity.User;

import java.util.List;

public interface RoadmapService {

    List<RoadmapResponseDto> getRoadmapsForCurrentUser();

    RoadmapDetailResponseDto getRoadmapForCurrentUser(Long roadmapId);

    void recordAcceptedProblem(User user, Long problemId);
}
