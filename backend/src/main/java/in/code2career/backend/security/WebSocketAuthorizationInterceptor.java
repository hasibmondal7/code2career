package in.code2career.backend.security;

import in.code2career.backend.entity.User;
import in.code2career.backend.repository.PvpRoomPlayerRepository;
import in.code2career.backend.repository.PvpRoomRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.JwtException;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

@Component
public class WebSocketAuthorizationInterceptor implements ChannelInterceptor {

    private static final String SUBMISSION_DESTINATION_PREFIX = "/topic/submissions/";
    private static final String PVP_DESTINATION_PREFIX = "/topic/pvp/rooms/";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final PvpRoomRepository roomRepository;
    private final PvpRoomPlayerRepository playerRepository;

    public WebSocketAuthorizationInterceptor(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            SubmissionRepository submissionRepository,
            PvpRoomRepository roomRepository,
            PvpRoomPlayerRepository playerRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            throw new AccessDeniedException("Invalid WebSocket message");
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new AccessDeniedException("Client messages are not supported");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("WebSocket authentication is required");
        }

        String token = authorization.substring(7);
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AccessDeniedException("WebSocket authentication failed");
        }

        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null || !jwtUtil.validateToken(token, user.getTokenVersion())) {
            throw new AccessDeniedException("WebSocket authentication failed");
        }

        accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(),
                null,
                List.of()
        ));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();
        if (principal == null || destination == null) {
            throw new AccessDeniedException("WebSocket subscription is not authorized");
        }

        if (destination.startsWith(SUBMISSION_DESTINATION_PREFIX)) {
            authorizeSubmissionSubscription(destination.substring(SUBMISSION_DESTINATION_PREFIX.length()), principal);
            return;
        }
        if (destination.startsWith(PVP_DESTINATION_PREFIX)) {
            authorizePvpSubscription(destination.substring(PVP_DESTINATION_PREFIX.length()), principal);
            return;
        }

        throw new AccessDeniedException("WebSocket subscription is not authorized");
    }

    private void authorizeSubmissionSubscription(String submissionId, Principal principal) {
        try {
            long id = Long.parseLong(submissionId);
            if (submissionRepository.existsByIdAndUser_EmailIgnoreCase(id, principal.getName())) {
                return;
            }
        } catch (NumberFormatException ignored) {
            // Invalid destination IDs are denied below.
        }
        throw new AccessDeniedException("WebSocket subscription is not authorized");
    }

    private void authorizePvpSubscription(String inviteCode, Principal principal) {
        String normalizedCode = inviteCode.toUpperCase(Locale.ROOT);
        if (!normalizedCode.matches("[A-Z0-9]{8}")) {
            throw new AccessDeniedException("WebSocket subscription is not authorized");
        }

        roomRepository.findByInviteCode(normalizedCode)
                .filter(room -> playerRepository.existsByRoom_IdAndUser_EmailIgnoreCase(
                        room.getId(),
                        principal.getName()
                ))
                .orElseThrow(() -> new AccessDeniedException("WebSocket subscription is not authorized"));
    }
}
