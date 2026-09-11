package in.code2career.backend.service.impl;

import in.code2career.backend.dto.LeaderboardEntryDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final LeaderboardServiceImpl leaderboardService = new LeaderboardServiceImpl(userRepository);

    @Test
    void mapsUsersToLeaderboardEntries() {
        User user = User.builder()
                .username("Hasib")
                .xp(100)
                .level(2)
                .currentStreak(4)
                .build();
        when(userRepository.findTop10ByOrderByTotalXpDesc()).thenReturn(List.of(user));

        List<LeaderboardEntryDto> result = leaderboardService.getTopUsers();

        assertEquals(1, result.size());
        assertEquals("Hasib", result.get(0).username());
        assertEquals(100, result.get(0).totalXp());
        assertEquals(2, result.get(0).level());
        assertEquals(4, result.get(0).currentStreak());
    }
}
