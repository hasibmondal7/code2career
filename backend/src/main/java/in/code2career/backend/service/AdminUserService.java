package in.code2career.backend.service;

import in.code2career.backend.dto.AdminUserUpdateDto;
import in.code2career.backend.dto.AdminUserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<AdminUserResponseDto> findUsers(String query, String role, Boolean active, Pageable pageable);
    AdminUserResponseDto getUser(Long userId);
    AdminUserResponseDto updateUser(Long userId, AdminUserUpdateDto update);
    void deleteUser(Long userId);
}
