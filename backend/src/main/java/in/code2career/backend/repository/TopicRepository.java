package in.code2career.backend.repository;

import in.code2career.backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByNameIgnoreCase(String name);
}
