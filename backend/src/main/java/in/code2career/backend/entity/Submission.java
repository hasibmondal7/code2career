package in.code2career.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
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

    @Column(nullable = false)
    private String language; // "java", "cpp", "python"

    @Column(nullable = false)
    private String status; // "Pending", "Accepted", "Wrong Answer", "Error"

    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now(); // Database-e save hobar shomoy automatic time set korbe
    }

    // Kon problem-er jonne submit koreche
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "email", "xp", "level", "createdAt"})
    private User user;
}