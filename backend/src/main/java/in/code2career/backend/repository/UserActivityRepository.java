package in.code2career.backend.repository;

import in.code2career.backend.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    Optional<UserActivity> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);

    List<UserActivity> findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(
            Long userId,
            LocalDate from,
            LocalDate to
    );
}
