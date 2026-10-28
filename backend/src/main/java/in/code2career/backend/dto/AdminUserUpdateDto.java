package in.code2career.backend.dto;

import in.code2career.backend.enums.UserRole;

public record AdminUserUpdateDto(UserRole role, Boolean active) {

    public boolean hasChanges() {
        return role != null || active != null;
    }
}
