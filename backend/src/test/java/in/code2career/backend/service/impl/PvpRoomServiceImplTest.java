package in.code2career.backend.service.impl;

import in.code2career.backend.dto.CreatePvpRoomRequest;
import in.code2career.backend.dto.PvpSubmissionRequest;
import in.code2career.backend.dto.PvpRoomResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.PvpRoom;
import in.code2career.backend.entity.PvpRoomPlayer;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.Difficulty;
import in.code2career.backend.enums.PvpRoomStatus;
import in.code2career.backend.exception.ConflictException;
import in.code2career.backend.exception.ForbiddenException;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.PvpRoomPlayerRepository;
import in.code2career.backend.repository.PvpRoomRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.CodeEvaluationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PvpRoomServiceImplTest {
    private final PvpRoomRepository roomRepository = mock(PvpRoomRepository.class);
    private final PvpRoomPlayerRepository playerRepository = mock(PvpRoomPlayerRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProblemRepository problemRepository = mock(ProblemRepository.class);
    private final TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
    private final CodeEvaluationService codeEvaluationService = mock(CodeEvaluationService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final PvpRoomServiceImpl service = new PvpRoomServiceImpl(
            roomRepository, playerRepository, userRepository, problemRepository,
            testCaseRepository, codeEvaluationService, messagingTemplate
    );

    private User host;
    private User guest;
    private Problem problem;
    private PvpRoom room;

    @BeforeEach
    void setUp() {
        host = User.builder().id(1L).username("host").email("host@example.com").build();
        guest = User.builder().id(2L).username("guest").email("guest@example.com").build();
        problem = Problem.builder()
                .id(10L)
                .title("Two Sum")
                .difficulty(Difficulty.EASY)
                .build();
        room = PvpRoom.builder()
                .id(20L)
                .inviteCode("ABCD2345")
                .roomName("Friday battle")
                .status(PvpRoomStatus.WAITING)
                .timeLimitMinutes(15)
                .maxPlayers(2)
                .stakeCoins(10)
                .language("JAVA")
                .publicMatch(false)
                .problem(problem)
                .host(host)
                .createdAt(LocalDateTime.now())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("host@example.com", "n/a", List.of())
        );
        when(userRepository.findByEmailIgnoreCase("host@example.com")).thenReturn(host);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(host));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(guest));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsRoomWithHostAsFirstPlayerAndReturnsInviteCode() {
        when(problemRepository.findById(10L)).thenReturn(Optional.of(problem));
        when(roomRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(roomRepository.save(any(PvpRoom.class))).thenAnswer(invocation -> {
            PvpRoom saved = invocation.getArgument(0);
            saved.setId(20L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        when(playerRepository.save(any(PvpRoomPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.findByRoomIdOrderByJoinedAtAsc(20L)).thenReturn(List.of(
                PvpRoomPlayer.builder().room(room).user(host).joinedAt(LocalDateTime.now()).build()
        ));

        var response = service.createRoom(new CreatePvpRoomRequest(" Friday battle ", 10L, 15, 2, 10, "JAVA", false));

        assertEquals("Friday battle", response.roomName());
        assertEquals(8, response.inviteCode().length());
        assertEquals(PvpRoomStatus.WAITING, response.status());
        assertEquals(1, response.players().size());
        assertTrue(response.players().get(0).host());
        verify(playerRepository).save(argThat(player -> player.getUser().equals(host)));
    }

    @Test
    void rejectsStartingRoomUntilAnotherPlayerHasJoined() {
        when(roomRepository.findByInviteCodeForUpdate("ABCD2345")).thenReturn(Optional.of(room));
        when(playerRepository.countByRoomId(20L)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.startRoom("abcd2345"));

        assertEquals(PvpRoomStatus.WAITING, room.getStatus());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(PvpRoomResponseDto.class));
    }

    @Test
    void joinsWaitingRoomAndPublishesUpdatedPlayerList() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("guest@example.com", "n/a", List.of())
        );
        when(userRepository.findByEmailIgnoreCase("guest@example.com")).thenReturn(guest);
        when(roomRepository.findByInviteCodeForUpdate("ABCD2345")).thenReturn(Optional.of(room));
        when(playerRepository.existsByRoomIdAndUserId(20L, 2L)).thenReturn(false);
        when(playerRepository.countByRoomId(20L)).thenReturn(1L);
        when(playerRepository.save(any(PvpRoomPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.findByRoomIdOrderByJoinedAtAsc(20L)).thenReturn(List.of(
                PvpRoomPlayer.builder().room(room).user(host).joinedAt(LocalDateTime.now()).build(),
                PvpRoomPlayer.builder().room(room).user(guest).joinedAt(LocalDateTime.now()).build()
        ));

        var response = service.joinRoom("abcd2345");

        assertEquals(2, response.players().size());
        assertEquals("guest", response.players().get(1).username());
        verify(playerRepository).save(argThat(player -> player.getUser().equals(guest)));
        verify(messagingTemplate).convertAndSend("/topic/pvp/rooms/ABCD2345", response);
    }

    @Test
    void rejectsJoiningFullRoom() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("guest@example.com", "n/a", List.of())
        );
        when(userRepository.findByEmailIgnoreCase("guest@example.com")).thenReturn(guest);
        when(roomRepository.findByInviteCodeForUpdate("ABCD2345")).thenReturn(Optional.of(room));
        when(playerRepository.existsByRoomIdAndUserId(20L, 2L)).thenReturn(false);
        when(playerRepository.countByRoomId(20L)).thenReturn(4L);

        assertThrows(ConflictException.class, () -> service.joinRoom("ABCD2345"));

        verify(playerRepository, never()).save(any(PvpRoomPlayer.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(PvpRoomResponseDto.class));
    }

    @Test
    void startsRoomAndBroadcastsItsDeadlineWhenAtLeastTwoPlayersJoined() {
        when(roomRepository.findByInviteCodeForUpdate("ABCD2345")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(20L)).thenReturn(List.of(
                PvpRoomPlayer.builder().room(room).user(host).stakePaid(true).build(),
                PvpRoomPlayer.builder().room(room).user(guest).stakePaid(true).build()
        ));
        when(roomRepository.save(room)).thenReturn(room);
        when(playerRepository.findByRoomIdOrderByJoinedAtAsc(20L)).thenReturn(List.of(
                PvpRoomPlayer.builder().room(room).user(host).joinedAt(LocalDateTime.now()).build(),
                PvpRoomPlayer.builder().room(room).user(guest).joinedAt(LocalDateTime.now()).build()
        ));

        var response = service.startRoom("ABCD2345");

        assertEquals(PvpRoomStatus.IN_PROGRESS, response.status());
        assertNotNull(response.startedAt());
        assertEquals(response.startedAt().plusMinutes(15), response.endsAt());
        verify(messagingTemplate).convertAndSend("/topic/pvp/rooms/ABCD2345", response);
    }

    @Test
    void onlyHostCanStartRoom() {
        User guest = User.builder().id(2L).username("guest").email("guest@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("guest@example.com", "n/a", List.of())
        );
        when(userRepository.findByEmailIgnoreCase("guest@example.com")).thenReturn(guest);
        when(roomRepository.findByInviteCodeForUpdate("ABCD2345")).thenReturn(Optional.of(room));

        assertThrows(ForbiddenException.class, () -> service.startRoom("ABCD2345"));

        verify(playerRepository, never()).countByRoomId(anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(PvpRoomResponseDto.class));
    }

    @Test
    void acceptedSubmissionAwardsWinnerThePotAndDifficultyXp() {
        host.setCoins(90);
        room.setStatus(PvpRoomStatus.IN_PROGRESS);
        room.setEndsAt(LocalDateTime.now().plusMinutes(5));
        when(playerRepository.existsByRoomIdAndUserId(20L, 1L)).thenReturn(true);
        when(testCaseRepository.findByProblemId(10L)).thenReturn(List.of(
                in.code2career.backend.entity.TestCase.builder().inputData("1").expectedOutput("1").build()
        ));
        when(codeEvaluationService.evaluate(eq("JAVA"), anyString(), anyList()))
                .thenReturn(new in.code2career.backend.dto.EvaluationResult("ACCEPTED", 8L));
        when(roomRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(room));
        when(roomRepository.findByInviteCodeForUpdate("ABCD2345")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(20L)).thenReturn(List.of(
                PvpRoomPlayer.builder().room(room).user(host).stakePaid(true).build(),
                PvpRoomPlayer.builder().room(room).user(guest).stakePaid(true).build()
        ));
        when(playerRepository.findByRoomIdOrderByJoinedAtAsc(20L)).thenReturn(List.of(
                PvpRoomPlayer.builder().room(room).user(host).joinedAt(LocalDateTime.now()).build(),
                PvpRoomPlayer.builder().room(room).user(guest).joinedAt(LocalDateTime.now()).build()
        ));

        var response = service.submitCode("ABCD2345", new PvpSubmissionRequest("class Solution {}", "JAVA"));

        assertEquals("ACCEPTED", response.status());
        assertTrue(response.winner());
        assertEquals(20, response.coinsWon());
        assertEquals(25, response.xpWon());
        assertEquals(110, host.getCoins());
        assertEquals(25, host.getXp());
        assertEquals(PvpRoomStatus.FINISHED, room.getStatus());
        assertEquals(host, room.getWinner());
    }

    @Test
    void rejectsRoomCreationWhenHostCannotCoverTheStake() {
        host.setCoins(0);

        assertThrows(ConflictException.class, () -> service.createRoom(
                new CreatePvpRoomRequest("Friday battle", 10L, 15, 2, 10, "JAVA", true)
        ));

        verify(problemRepository, never()).findById(anyLong());
        verify(roomRepository, never()).save(any(PvpRoom.class));
    }
}
