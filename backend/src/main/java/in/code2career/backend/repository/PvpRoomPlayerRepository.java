package in.code2career.backend.repository;

import in.code2career.backend.entity.PvpRoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PvpRoomPlayerRepository extends JpaRepository<PvpRoomPlayer, Long> {
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomId(Long roomId);

    List<PvpRoomPlayer> findByRoomIdOrderByJoinedAtAsc(Long roomId);

    List<PvpRoomPlayer> findByRoomId(Long roomId);
}
