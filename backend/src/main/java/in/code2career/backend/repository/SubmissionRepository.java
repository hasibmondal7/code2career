package in.code2career.backend.repository;

import in.code2career.backend.entity.Submission;
import in.code2career.backend.enums.SubmissionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserId(Long userId);
    List<Submission> findByProblemId(Long problemId);

    long countByUserIdAndStatus(Long userId, SubmissionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Submission s WHERE s.id = :id")
    Optional<Submission> findByIdForUpdate(@Param("id") Long id);
}