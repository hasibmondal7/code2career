package in.code2career.backend.security;

import in.code2career.backend.entity.User;
import in.code2career.backend.entity.PvpRoom;
import in.code2career.backend.repository.PvpRoomPlayerRepository;
import in.code2career.backend.repository.PvpRoomRepository;
import in.code2career.backend.repository.SubmissionRepository;
import in.code2career.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthorizationInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final PvpRoomRepository roomRepository = mock(PvpRoomRepository.class);
    private final PvpRoomPlayerRepository playerRepository = mock(PvpRoomPlayerRepository.class);
    private final WebSocketAuthorizationInterceptor interceptor = new WebSocketAuthorizationInterceptor(
            jwtUtil,
            userRepository,
            submissionRepository,
            roomRepository,
            playerRepository
    );

    @Test
    void rejectsConnectWithoutBearerToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), mock(MessageChannel.class)));
    }

    @Test
    void rejectsClientPublishedMessages() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/submissions/24");

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                message(accessor),
                mock(MessageChannel.class)
        ));
    }

    @Test
    void authenticatesConnectUsingCurrentTokenVersion() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer valid-token");
        User user = User.builder().email("user@example.com").tokenVersion(2).build();
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(user);
        when(jwtUtil.validateToken("valid-token", 2)).thenReturn(true);

        interceptor.preSend(message(accessor), mock(MessageChannel.class));

        assertEquals("user@example.com", accessor.getUser().getName());
    }

    @Test
    void permitsSubmissionStatusSubscriptionOnlyForItsOwner() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(
                "owner@example.com", null, List.of()
        ));
        accessor.setDestination("/topic/submissions/24");
        when(submissionRepository.existsByIdAndUser_EmailIgnoreCase(24L, "owner@example.com"))
                .thenReturn(true);

        interceptor.preSend(message(accessor), mock(MessageChannel.class));

        verify(submissionRepository).existsByIdAndUser_EmailIgnoreCase(24L, "owner@example.com");
    }

    @Test
    void deniesSubmissionStatusSubscriptionForAnotherUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(
                "other@example.com", null, List.of()
        ));
        accessor.setDestination("/topic/submissions/24");
        when(submissionRepository.existsByIdAndUser_EmailIgnoreCase(24L, "other@example.com"))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                message(accessor),
                mock(MessageChannel.class)
        ));
    }

    @Test
    void deniesPvpSubscriptionForNonMember() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(
                "other@example.com", null, List.of()
        ));
        accessor.setDestination("/topic/pvp/rooms/ABCD2345");
        when(roomRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                message(accessor),
                mock(MessageChannel.class)
        ));
    }

    @Test
    void permitsPvpSubscriptionForRoomMember() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(
                "member@example.com", null, List.of()
        ));
        accessor.setDestination("/topic/pvp/rooms/ABCD2345");
        when(roomRepository.findByInviteCode("ABCD2345"))
                .thenReturn(Optional.of(PvpRoom.builder().id(12L).build()));
        when(playerRepository.existsByRoom_IdAndUser_EmailIgnoreCase(12L, "member@example.com"))
                .thenReturn(true);

        interceptor.preSend(message(accessor), mock(MessageChannel.class));

        verify(playerRepository).existsByRoom_IdAndUser_EmailIgnoreCase(12L, "member@example.com");
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
