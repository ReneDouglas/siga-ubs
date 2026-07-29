package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;

public interface SystemAdminRepository extends JpaRepository<SystemAdmin, Long> {

    @Transactional(readOnly = true)
    Optional<SystemAdmin> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT admin FROM SystemAdmin admin WHERE admin.active = true ORDER BY admin.id")
    List<SystemAdmin> findAllActiveForUpdate();

    @Transactional(readOnly = true)
    @Query(value = """
            SELECT admin FROM SystemAdmin admin
            WHERE (:username IS NULL OR LOWER(admin.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:name IS NULL OR LOWER(admin.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:active IS NULL OR admin.active = :active)
            """,
            countQuery = """
            SELECT COUNT(admin.id) FROM SystemAdmin admin
            WHERE (:username IS NULL OR LOWER(admin.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:name IS NULL OR LOWER(admin.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:active IS NULL OR admin.active = :active)
            """)
    Page<SystemAdmin> findByFilters(@Param("username") String username,
            @Param("name") String name,
            @Param("active") Boolean active,
            Pageable pageable);
}
