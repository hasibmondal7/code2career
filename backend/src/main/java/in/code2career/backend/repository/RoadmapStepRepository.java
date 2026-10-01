package in.code2career.backend.repository;

import in.code2career.backend.entity.RoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadmapStepRepository extends JpaRepository<RoadmapStep, Long> {

    List<RoadmapStep> findByRoadmapIdOrderByStepOrderAsc(Long roadmapId);

    List<RoadmapStep> findByProblemId(Long problemId);

    List<RoadmapStep> findAllByOrderByRoadmap_IdAscStepOrderAsc();
}
