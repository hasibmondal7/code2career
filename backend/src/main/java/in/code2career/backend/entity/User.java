package in.code2career.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.code2career.backend.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "profile_photo", columnDefinition = "TEXT")
    private String profilePhoto;

    @Column(name = "xp", nullable = false)
    @Builder.Default
    private Integer xp = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (xp == null) {
            xp = 0;
        }
        if (level == null) {
            level = 1;
        }
        if (currentStreak == null) {
            currentStreak = 0;
        }
        if (role == null) {
            role = UserRole.USER;
        }
    }

    // Helper method to add XP and calculate level dynamically
    public void addXp(Integer xpToAdd) {
        if (this.xp == null) {
            this.xp = 0;
        }
        this.xp += xpToAdd;

        // Calculate Level: Level L requires 50 * L * (L-1) XP
        int calculatedLevel = 1;
        while (this.xp >= 50 * calculatedLevel * (calculatedLevel + 1)) {
            calculatedLevel++;
        }
        this.level = calculatedLevel;
    }
}