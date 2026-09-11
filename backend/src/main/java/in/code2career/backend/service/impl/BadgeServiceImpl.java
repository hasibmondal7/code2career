package in.code2career.backend.service.impl;

import in.code2career.backend.dto.BadgeResponseDto;
import in.code2career.backend.entity.Badge;
import in.code2career.backend.entity.User;
import in.code2career.backend.entity.UserBadge;
import in.code2career.backend.exception.InvalidCredentialsException;
import in.code2career.backend.repository.BadgeRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.UserBadgeRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.BadgeService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public BadgeServiceImpl(
            BadgeRepository badgeRepository,
            UserBadgeRepository userBadgeRepository,
            SubmissionRepository submissionRepository,
            UserRepository userRepository
    ) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void awardEligibleBadges(User user) {
        long solvedCount = submissionRepository.countByUserIdAndStatus(
                user.getId(),
                in.code2career.backend.enums.SubmissionStatus.ACCEPTED
        );
        if (solvedCount >= 1) {
            award(user, "FIRST_CODE");
        }
        if (solvedCount >= 10) {
            award(user, "TEN_PROBLEMS");
        }
        if (user.getXp() >= 100) {
            award(user, "HUNDRED_XP");
        }
        if (user.getCurrentStreak() >= 7) {
            award(user, "SEVEN_DAY_STREAK");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponseDto> getCurrentUserBadges() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null) {
            throw new InvalidCredentialsException();
        }

        return userBadgeRepository.findByUserIdOrderByAwardedAtDesc(user.getId())
                .stream()
                .map(userBadge -> new BadgeResponseDto(
                        userBadge.getBadge().getCode(),
                        userBadge.getBadge().getName(),
                        userBadge.getBadge().getDescription(),
                        userBadge.getAwardedAt()
                ))
                .toList();
    }

    private void award(User user, String code) {
        Badge badge = badgeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Badge is not configured: " + code));
        if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
            userBadgeRepository.save(UserBadge.builder().user(user).badge(badge).build());
        }
    }
}
