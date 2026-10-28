package in.code2career.backend.controller;

import in.code2career.backend.dto.AdminUserUpdateDto;
import in.code2career.backend.dto.AdminUserResponseDto;
import in.code2career.backend.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) { this.adminUserService = adminUserService; }

    @GetMapping
    public Page<AdminUserResponseDto> findUsers(@RequestParam(defaultValue = "") String query,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) Boolean active,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminUserService.findUsers(query, role, active, PageRequest.of(page, size));
    }

    @GetMapping("/{userId}")
    public AdminUserResponseDto getUser(@PathVariable Long userId) { return adminUserService.getUser(userId); }

    @PatchMapping("/{userId}")
    public AdminUserResponseDto updateUser(@PathVariable Long userId, @Valid @RequestBody AdminUserUpdateDto update) {
        return adminUserService.updateUser(userId, update);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
