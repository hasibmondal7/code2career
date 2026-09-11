package in.code2career.backend.service;

import in.code2career.backend.dto.BadgeResponseDto;
import in.code2career.backend.entity.User;

import java.util.List;

public interface BadgeService {

    void awardEligibleBadges(User user);

    List<BadgeResponseDto> getCurrentUserBadges();
}
