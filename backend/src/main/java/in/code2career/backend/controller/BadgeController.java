package in.code2career.backend.controller;

import in.code2career.backend.dto.BadgeResponseDto;
import in.code2career.backend.service.BadgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping
    public List<BadgeResponseDto> getBadges() {
        return badgeService.getCurrentUserBadges();
    }
}
