package in.code2career.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roadmaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String icon;

    @Column(nullable = false, length = 30)
    private String accent;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
