package in.code2career.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "user_activity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_activity_date",
                columnNames = {"user_id", "activity_date"}
        ),
        indexes = @Index(name = "idx_user_activity_user_date", columnList = "user_id, activity_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer problemsSolved = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer acceptedSubmissions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer xpEarned = 0;
}
