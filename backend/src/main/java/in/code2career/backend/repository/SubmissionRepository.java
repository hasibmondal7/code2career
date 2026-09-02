package in.code2career.backend.repository;

import in.code2career.backend.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserId(Long userId); // Ekjon user-er shob submission dekhar jonne
    List<Submission> findByProblemId(Long problemId); // Ekta problem-e kotojon submit koreche
}