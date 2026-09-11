package in.code2career.backend.service.impl;

import in.code2career.backend.dto.LeaderboardEntryDto;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.LeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final UserRepository userRepository;

    public LeaderboardServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getTopUsers() {
        return userRepository.findTop10ByOrderByTotalXpDesc()
                .stream()
                .map(user -> new LeaderboardEntryDto(
                        user.getUsername(),
                        user.getXp(),
                        user.getLevel(),
                        user.getCurrentStreak()
                ))
                .toList();
    }
}
