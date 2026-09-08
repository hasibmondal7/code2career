package in.code2career.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.code2career.backend.enums.Difficulty;

@Entity
@Table(
        name = "problems",
        indexes = {
                @Index(name = "idx_problem_topic", columnList = "topic_id"),
                @Index(name = "idx_problem_difficulty", columnList = "difficulty")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_id")
    @JsonIgnoreProperties({"password", "email", "createdAt", "xp", "level"})
    private User addedBy;
}
