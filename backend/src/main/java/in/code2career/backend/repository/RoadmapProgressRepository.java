package in.code2career.backend.repository;

import in.code2career.backend.entity.RoadmapProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadmapProgressRepository extends JpaRepository<RoadmapProgress, Long> {

    List<RoadmapProgress> findByUserId(Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO roadmap_progress (user_id, roadmap_step_id, completed_at)
            VALUES (:userId, :stepId, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, roadmap_step_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("stepId") Long stepId);
}
