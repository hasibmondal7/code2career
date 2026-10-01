package in.code2career.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pvp_room_players",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pvp_room_player",
                columnNames = {"room_id", "user_id"}
        ),
        indexes = @Index(name = "idx_pvp_room_players_room", columnList = "room_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PvpRoomPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private PvpRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "stake_paid", nullable = false)
    @Builder.Default
    private Boolean stakePaid = false;

    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
