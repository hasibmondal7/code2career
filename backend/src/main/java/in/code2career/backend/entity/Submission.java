package in.code2career.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.code2career.backend.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "submissions",
        indexes = {
                @Index(name = "idx_submission_problem", columnList = "problem_id"),
                @Index(name = "idx_submission_user", columnList = "user_id"),
                @Index(name = "idx_submission_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code; // User-er lekha ashol code (jemon: class Solution { ... })

    @Column(nullable = false, length = 30)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status;

    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "email", "xp", "level", "createdAt"})
    private User user;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "xp_awarded")
    @Builder.Default
    private Boolean xpAwarded = false;
}
