package in.code2career.backend.repository;

import in.code2career.backend.entity.PvpRoom;
import in.code2career.backend.enums.PvpRoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface PvpRoomRepository extends JpaRepository<PvpRoom, Long> {
    boolean existsByInviteCode(String inviteCode);

    Optional<PvpRoom> findByInviteCode(String inviteCode);

    List<PvpRoom> findByStatusAndEndsAtBefore(PvpRoomStatus status, LocalDateTime time);

    List<PvpRoom> findByStatusAndPublicMatchTrueOrderByCreatedAtAsc(PvpRoomStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PvpRoom r where r.inviteCode = :inviteCode")
    Optional<PvpRoom> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);
}
