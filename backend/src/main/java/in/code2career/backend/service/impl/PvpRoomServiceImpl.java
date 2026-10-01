package in.code2career.backend.service.impl;

import in.code2career.backend.dto.CreatePvpRoomRequest;
import in.code2career.backend.dto.PvpRoomPlayerResponseDto;
import in.code2career.backend.dto.PvpRoomResponseDto;
import in.code2career.backend.dto.PvpSubmissionRequest;
import in.code2career.backend.dto.PvpSubmissionResponse;
import in.code2career.backend.dto.EvaluationResult;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.PvpRoom;
import in.code2career.backend.entity.PvpRoomPlayer;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.PvpRoomStatus;
import in.code2career.backend.exception.ConflictException;
import in.code2career.backend.exception.ForbiddenException;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.repository.ProblemRepository;
import in.code2career.backend.repository.PvpRoomPlayerRepository;
import in.code2career.backend.repository.PvpRoomRepository;
import in.code2career.backend.repository.TestCaseRepository;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.CodeEvaluationService;
import in.code2career.backend.service.PvpRoomService;
import in.code2career.backend.enums.Difficulty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PvpRoomServiceImpl implements PvpRoomService {
    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PvpRoomRepository roomRepository;
    private final PvpRoomPlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeEvaluationService codeEvaluationService;
    private final SimpMessagingTemplate messagingTemplate;

    public PvpRoomServiceImpl(
            PvpRoomRepository roomRepository,
            PvpRoomPlayerRepository playerRepository,
            UserRepository userRepository,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            CodeEvaluationService codeEvaluationService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeEvaluationService = codeEvaluationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public PvpRoomResponseDto createRoom(CreatePvpRoomRequest request) {
        User host = currentUser();
        if (host.getCoins() < request.stakeCoins()) {
            throw new ConflictException("Not enough coins to enter this battle");
        }
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", request.problemId()));
        PvpRoom room = PvpRoom.builder()
                .inviteCode(generateInviteCode())
                .roomName(request.roomName().trim())
                .status(PvpRoomStatus.WAITING)
                .timeLimitMinutes(request.timeLimitMinutes())
                .maxPlayers(request.maxPlayers())
                .stakeCoins(request.stakeCoins())
                .language(request.language().toUpperCase())
                .publicMatch(request.publicMatch())
                .problem(problem)
                .host(host)
                .build();
        room = roomRepository.save(room);
        playerRepository.save(PvpRoomPlayer.builder().room(room).user(host).build());
        return toResponse(room);
    }

    @Override
    @Transactional
    public PvpRoomResponseDto joinRoom(String inviteCode) {
        User user = currentUser();
        PvpRoom room = roomRepository.findByInviteCodeForUpdate(normalizeInviteCode(inviteCode))
                .orElseThrow(() -> new ResourceNotFoundException("PvP room", "invite code", inviteCode));
        boolean alreadyMember = playerRepository.existsByRoomIdAndUserId(room.getId(), user.getId());
        if (room.getStatus() != PvpRoomStatus.WAITING && !alreadyMember) {
            throw new ConflictException("This room is no longer accepting players");
        }

        if (!alreadyMember) {
            if (user.getCoins() < room.getStakeCoins()) {
                throw new ConflictException("Not enough coins to accept this battle");
            }
            if (playerRepository.countByRoomId(room.getId()) >= room.getMaxPlayers()) {
                throw new ConflictException("This room is full");
            }
            playerRepository.save(PvpRoomPlayer.builder().room(room).user(user).build());
        }

        PvpRoomResponseDto response = toResponse(room);
        publishRoomUpdate(room, response);
        return response;
    }

    @Override
    @Transactional
    public PvpRoomResponseDto getRoom(String inviteCode) {
        User user = currentUser();
        PvpRoom room = findRoom(inviteCode);
        requireMember(room, user);
        if (room.getStatus() == PvpRoomStatus.IN_PROGRESS && room.getEndsAt() != null
                && !room.getEndsAt().isAfter(LocalDateTime.now())) {
            room = finishAsDraw(room.getInviteCode());
        }
        return toResponse(room);
    }

    @Override
    @Transactional
    public PvpRoomResponseDto startRoom(String inviteCode) {
        User user = currentUser();
        PvpRoom room = roomRepository.findByInviteCodeForUpdate(normalizeInviteCode(inviteCode))
                .orElseThrow(() -> new ResourceNotFoundException("PvP room", "invite code", inviteCode));
        if (!room.getHost().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the room host can start the battle");
        }
        if (room.getStatus() != PvpRoomStatus.WAITING) {
            throw new ConflictException("This room has already been started");
        }
        List<PvpRoomPlayer> players = playerRepository.findByRoomId(room.getId());
        if (players.size() != 2) {
            throw new ConflictException("Exactly two players are required to start a battle");
        }
        List<PvpRoomPlayer> lockedPlayers = players.stream()
                .sorted((left, right) -> left.getUser().getId().compareTo(right.getUser().getId()))
                .toList();
        List<User> participants = lockedPlayers.stream()
                .map(player -> userRepository.findByIdForUpdate(player.getUser().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", player.getUser().getId())))
                .toList();
        for (User participant : participants) {
            if (participant.getCoins() < room.getStakeCoins()) {
                throw new ConflictException(participant.getUsername() + " does not have enough coins");
            }
        }
        for (int index = 0; index < lockedPlayers.size(); index++) {
            PvpRoomPlayer player = lockedPlayers.get(index);
            User participant = participants.get(index);
            participant.setCoins(participant.getCoins() - room.getStakeCoins());
            userRepository.save(participant);
            player.setStakePaid(true);
            playerRepository.save(player);
        }

        LocalDateTime startedAt = LocalDateTime.now();
        room.setStatus(PvpRoomStatus.IN_PROGRESS);
        room.setStartedAt(startedAt);
        room.setEndsAt(startedAt.plusMinutes(room.getTimeLimitMinutes()));
        roomRepository.save(room);

        PvpRoomResponseDto response = toResponse(room);
        publishRoomUpdate(room, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PvpRoomResponseDto> getOpenRooms() {
        User user = currentUser();
        return roomRepository.findByStatusAndPublicMatchTrueOrderByCreatedAtAsc(PvpRoomStatus.WAITING).stream()
                .filter(room -> !room.getHost().getId().equals(user.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Scheduled(fixedDelayString = "${app.pvp.expiry-check-ms:10000}")
    @Transactional
    public void settleExpiredMatches() {
        roomRepository.findByStatusAndEndsAtBefore(PvpRoomStatus.IN_PROGRESS, LocalDateTime.now())
                .forEach(room -> finishAsDraw(room.getInviteCode()));
    }

    @Override
    @Transactional
    public PvpSubmissionResponse submitCode(String inviteCode, PvpSubmissionRequest request) {
        User user = currentUser();
        PvpRoom room = findRoom(inviteCode);
        requireMember(room, user);
        if (room.getStatus() != PvpRoomStatus.IN_PROGRESS) {
            throw new ConflictException("This battle is not accepting submissions");
        }
        if (!room.getEndsAt().isAfter(LocalDateTime.now())) {
            finishAsDraw(room.getInviteCode());
            throw new ConflictException("The battle time has expired");
        }
        if (!room.getLanguage().equalsIgnoreCase(request.language())) {
            throw new ConflictException("Use the battle's selected language: " + room.getLanguage());
        }

        var testCases = testCaseRepository.findByProblemId(room.getProblem().getId());
        if (testCases.isEmpty()) {
            throw new ConflictException("This problem has no test cases and cannot be used for PvP yet");
        }
        EvaluationResult result = codeEvaluationService.evaluate(room.getLanguage(), request.code(), testCases);
        boolean won = false;
        int coinsWon = 0;
        int xpWon = 0;
        if ("ACCEPTED".equals(result.getStatus())) {
            PvpRoom lockedRoom = roomRepository.findByInviteCodeForUpdate(normalizeInviteCode(inviteCode))
                    .orElseThrow(() -> new ResourceNotFoundException("PvP room", "invite code", inviteCode));
            if (lockedRoom.getStatus() == PvpRoomStatus.IN_PROGRESS
                    && lockedRoom.getEndsAt().isAfter(LocalDateTime.now())) {
                List<PvpRoomPlayer> players = playerRepository.findByRoomId(lockedRoom.getId());
                PvpRoomPlayer winnerPlayer = players.stream()
                        .filter(player -> player.getUser().getId().equals(user.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ForbiddenException("You are not a player in this battle"));
                winnerPlayer.setSolvedAt(LocalDateTime.now());
                playerRepository.save(winnerPlayer);
                int participantsPaid = (int) players.stream().filter(player -> Boolean.TRUE.equals(player.getStakePaid())).count();
                coinsWon = participantsPaid * lockedRoom.getStakeCoins();
                xpWon = xpReward(lockedRoom.getProblem().getDifficulty());
                User winner = userRepository.findByIdForUpdate(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));
                winner.setCoins(winner.getCoins() + coinsWon);
                winner.addXp(xpWon);
                userRepository.save(winner);
                lockedRoom.setWinner(winner);
                lockedRoom.setStatus(PvpRoomStatus.FINISHED);
                roomRepository.save(lockedRoom);
                PvpRoomResponseDto response = toResponse(lockedRoom);
                publishRoomUpdate(lockedRoom, response);
                won = true;
            }
        }
        return new PvpSubmissionResponse(result.getStatus(), result.getExecutionTimeMs(), won, coinsWon, xpWon);
    }

    private PvpRoom findRoom(String inviteCode) {
        return roomRepository.findByInviteCode(normalizeInviteCode(inviteCode))
                .orElseThrow(() -> new ResourceNotFoundException("PvP room", "invite code", inviteCode));
    }

    private void requireMember(PvpRoom room, User user) {
        if (!playerRepository.existsByRoomIdAndUserId(room.getId(), user.getId())) {
            throw new ForbiddenException("Join this room to view its details");
        }
    }

    @Transactional
    protected PvpRoom finishAsDraw(String inviteCode) {
        PvpRoom room = roomRepository.findByInviteCodeForUpdate(normalizeInviteCode(inviteCode))
                .orElseThrow(() -> new ResourceNotFoundException("PvP room", "invite code", inviteCode));
        if (room.getStatus() != PvpRoomStatus.IN_PROGRESS) {
            return room;
        }
        List<PvpRoomPlayer> expiredPlayers = playerRepository.findByRoomId(room.getId()).stream()
                .sorted((left, right) -> left.getUser().getId().compareTo(right.getUser().getId()))
                .toList();
        for (PvpRoomPlayer player : expiredPlayers) {
            if (Boolean.TRUE.equals(player.getStakePaid())) {
                User participant = userRepository.findByIdForUpdate(player.getUser().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", player.getUser().getId()));
                participant.setCoins(participant.getCoins() + room.getStakeCoins());
                userRepository.save(participant);
                player.setStakePaid(false);
                playerRepository.save(player);
            }
        }
        room.setStatus(PvpRoomStatus.FINISHED);
        roomRepository.save(room);
        publishRoomUpdate(room, toResponse(room));
        return room;
    }

    private int xpReward(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 25;
            case MEDIUM -> 50;
            case HARD -> 100;
        };
    }

    private PvpRoomResponseDto toResponse(PvpRoom room) {
        List<PvpRoomPlayerResponseDto> players = playerRepository
                .findByRoomIdOrderByJoinedAtAsc(room.getId())
                .stream()
                .map(player -> new PvpRoomPlayerResponseDto(
                        player.getUser().getId(),
                        player.getUser().getUsername(),
                        player.getUser().getProfilePhoto(),
                        player.getUser().getId().equals(room.getHost().getId()),
                        player.getJoinedAt(),
                        player.getUser().getCoins()
                ))
                .toList();

        return new PvpRoomResponseDto(
                room.getInviteCode(),
                room.getRoomName(),
                room.getStatus(),
                room.getTimeLimitMinutes(),
                room.getMaxPlayers(),
                room.getStakeCoins(),
                room.getLanguage(),
                Boolean.TRUE.equals(room.isPublicMatch()),
                room.getProblem().getId(),
                room.getProblem().getTitle(),
                room.getProblem().getDifficulty().name(),
                room.getHost().getId(),
                room.getCreatedAt(),
                room.getStartedAt(),
                room.getEndsAt(),
                room.getWinner() == null ? null : room.getWinner().getId(),
                room.getWinner() == null ? null : xpReward(room.getProblem().getDifficulty()),
                players
        );
    }

    private void publishRoomUpdate(PvpRoom room, PvpRoomResponseDto response) {
        messagingTemplate.convertAndSend("/topic/pvp/rooms/" + room.getInviteCode(), response);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }
        return user;
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int index = 0; index < 8; index++) {
                code.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!roomRepository.existsByInviteCode(candidate)) {
                return candidate;
            }
        }
        throw new ConflictException("Could not generate a unique room invite code; please try again");
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode.trim().toUpperCase();
    }
}
