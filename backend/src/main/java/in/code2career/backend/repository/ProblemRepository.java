package in.code2career.backend.repository;

import in.code2career.backend.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    // Ekta nirdishto topic-er shob problem khuje anar method
    List<Problem> findByTopicId(Long topicId);
}