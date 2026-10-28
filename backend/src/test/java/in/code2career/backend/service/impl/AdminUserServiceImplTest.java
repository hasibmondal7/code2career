package in.code2career.backend.service.impl;

import in.code2career.backend.dto.AdminUserUpdateDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.UserRole;
import in.code2career.backend.exception.InvalidRequestException;
import in.code2career.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AdminUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserServiceImpl(userRepository, jdbcTemplate);
    }

    @Test
    void deactivatingLastActiveAdminIsRejected() {
        User admin = user(1L, UserRole.ADMIN);
        when(userRepository.findByRoleAndActiveTrueForUpdate(UserRole.ADMIN)).thenReturn(List.of(admin));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));

        assertThrows(InvalidRequestException.class,
                () -> service.updateUser(1L, new AdminUserUpdateDto(null, false)));

        verify(userRepository, never()).save(admin);
    }

    @Test
    void deactivationInvalidatesExistingTokens() {
        User admin = user(1L, UserRole.ADMIN);
        User secondAdmin = user(2L, UserRole.ADMIN);
        when(userRepository.findByRoleAndActiveTrueForUpdate(UserRole.ADMIN))
                .thenReturn(List.of(admin, secondAdmin));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(userRepository.save(admin)).thenReturn(admin);

        var updated = service.updateUser(1L, new AdminUserUpdateDto(null, false));

        assertFalse(updated.active());
        assertEquals(1, admin.getTokenVersion());
        verify(userRepository).save(admin);
    }

    @Test
    void deletionRemovesUserOwnedRecordsBeforeDeletingAccount() {
        User regularUser = user(3L, UserRole.USER);
        when(userRepository.findByRoleAndActiveTrueForUpdate(UserRole.ADMIN)).thenReturn(List.of());
        when(userRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(regularUser));

        service.deleteUser(3L);

        verify(jdbcTemplate).update("DELETE FROM user_activity WHERE user_id = ?", 3L);
        verify(jdbcTemplate).update("DELETE FROM submissions WHERE user_id = ?", 3L);
        verify(jdbcTemplate).update("UPDATE problems SET admin_id = NULL WHERE admin_id = ?", 3L);
        verify(userRepository).delete(regularUser);
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .username("member-" + id)
                .email("member-" + id + "@example.invalid")
                .password("encoded")
                .role(role)
                .active(true)
                .build();
    }
}
