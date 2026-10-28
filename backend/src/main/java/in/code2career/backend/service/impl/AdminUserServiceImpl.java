package in.code2career.backend.service.impl;

import in.code2career.backend.dto.AdminUserUpdateDto;
import in.code2career.backend.dto.AdminUserResponseDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.UserRole;
import in.code2career.backend.exception.InvalidRequestException;
import in.code2career.backend.exception.ResourceNotFoundException;
import in.code2career.backend.repository.UserRepository;
import in.code2career.backend.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminUserServiceImpl(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> findUsers(String query, String role, Boolean active, Pageable pageable) {
        UserRole parsedRole = parseRole(role);
        return userRepository.search(query == null ? "" : query.trim(), parsedRole, active, pageable)
                .map(AdminUserServiceImpl::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponseDto getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    @Override
    @Transactional
    public AdminUserResponseDto updateUser(Long userId, AdminUserUpdateDto update) {
        if (!update.hasChanges()) {
            throw new InvalidRequestException("Provide an account status or role to update");
        }
        List<User> activeAdmins = userRepository.findByRoleAndActiveTrueForUpdate(UserRole.ADMIN);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        boolean removesActiveAdmin = user.getRole() == UserRole.ADMIN && user.isActive()
                && ((update.role() != null && update.role() != UserRole.ADMIN) || Boolean.FALSE.equals(update.active()));
        if (removesActiveAdmin) {
            ensureAnotherActiveAdminExists(user, activeAdmins);
        }
        if (update.role() != null) user.setRole(update.role());
        if (update.active() != null) {
            user.setActive(update.active());
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        User saved = userRepository.save(user);
        log.info("Administrator updated account id {}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        List<User> activeAdmins = userRepository.findByRoleAndActiveTrueForUpdate(UserRole.ADMIN);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() == UserRole.ADMIN && user.isActive()) {
            ensureAnotherActiveAdminExists(user, activeAdmins);
        }
        removeUserReferences(user);
        userRepository.delete(user);
        log.warn("Administrator deleted account id {}", userId);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private void ensureAnotherActiveAdminExists(User user, List<User> activeAdmins) {
        long otherAdmins = activeAdmins.stream()
                .filter(admin -> !admin.getId().equals(user.getId()))
                .count();
        if (otherAdmins == 0) {
            throw new InvalidRequestException("The last active administrator cannot be deactivated, demoted, or deleted");
        }
    }

    private void removeUserReferences(User user) {
        Long userId = user.getId();
        jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE LOWER(email) = LOWER(?)", user.getEmail());
        jdbcTemplate.update("DELETE FROM user_badges WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_activity WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM roadmap_progress WHERE user_id = ?", userId);
        jdbcTemplate.update("UPDATE pvp_rooms SET winner_id = NULL WHERE winner_id = ?", userId);
        jdbcTemplate.update("DELETE FROM pvp_rooms WHERE host_id = ?", userId);
        jdbcTemplate.update("DELETE FROM pvp_room_players WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM submissions WHERE user_id = ?", userId);
        jdbcTemplate.update("UPDATE topics SET admin_id = NULL WHERE admin_id = ?", userId);
        jdbcTemplate.update("UPDATE problems SET admin_id = NULL WHERE admin_id = ?", userId);
    }

    private static AdminUserResponseDto toResponse(User user) {
        return new AdminUserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getXp(),
                user.getLevel(),
                user.getCurrentStreak(),
                user.getCreatedAt()
        );
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) return null;
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Role must be USER or ADMIN");
        }
    }
}
