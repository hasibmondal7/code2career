package in.code2career.backend.controller;

import in.code2career.backend.dto.RoadmapDetailResponseDto;
import in.code2career.backend.dto.RoadmapResponseDto;
import in.code2career.backend.service.RoadmapService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @GetMapping
    public List<RoadmapResponseDto> getRoadmaps() {
        return roadmapService.getRoadmapsForCurrentUser();
    }

    @GetMapping("/{roadmapId}")
    public RoadmapDetailResponseDto getRoadmap(@PathVariable Long roadmapId) {
        return roadmapService.getRoadmapForCurrentUser(roadmapId);
    }
}
