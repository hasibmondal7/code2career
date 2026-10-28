package in.code2career.backend.repository;

import in.code2career.backend.entity.User;
import in.code2career.backend.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findTop10ByOrderByXpDesc();

    User findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true ORDER BY u.id")
    List<User> findByRoleAndActiveTrueForUpdate(@Param("role") UserRole role);

    @Query("""
            SELECT u FROM User u
            WHERE (:query = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:active IS NULL OR u.active = :active)
            """)
    Page<User> search(
            @Param("query") String query,
            @Param("role") UserRole role,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
