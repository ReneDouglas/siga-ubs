package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.SystemUser;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

//@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, Long>, SystemUserRepositoryCustom {

    @EntityGraph(value = "SystemUserGraph")
    @Transactional(readOnly = true)
    Optional<SystemUser> findByUsername(String username);

    @EntityGraph(value = "SystemUserGraph")
    @Transactional(readOnly = true)
    List<SystemUser> findAllByCreationUser(String creationUser);

    @Override
    @EntityGraph(attributePaths = {"roles", "basicHealthUnit"})
    @Transactional(readOnly = true)
    <S extends SystemUser> Page<S> findAll(Example<S> example, Pageable pageable);

    @Override
    @EntityGraph(value = "SystemUserGraph")
    //@QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
    Optional<SystemUser> findById(Long aLong);

    @Transactional(readOnly = true)
    @Query(value = """
            SELECT DISTINCT su FROM SystemUser su
                JOIN su.roles r
            WHERE r.role = :role
              AND (:username IS NULL OR LOWER(su.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:name IS NULL OR LOWER(su.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:active IS NULL OR su.active = :active)
            """,
            countQuery = """
            SELECT COUNT(DISTINCT su.id) FROM SystemUser su
                JOIN su.roles r
            WHERE r.role = :role
              AND (:username IS NULL OR LOWER(su.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:name IS NULL OR LOWER(su.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:active IS NULL OR su.active = :active)
            """)
    Page<SystemUser> findByRoleAndFilters(@Param("role") String role,
            @Param("username") String username,
            @Param("name") String name,
            @Param("active") Boolean active,
            Pageable pageable);

    @Transactional(readOnly = true)
    @Query(value = """
            SELECT su.id FROM SystemUser su
                JOIN su.roles r
            WHERE r.role = :role
              AND (:username IS NULL OR LOWER(su.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:name IS NULL OR LOWER(su.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:active IS NULL OR su.active = :active)
            """,
            countQuery = """
            SELECT COUNT(su.id) FROM SystemUser su
                JOIN su.roles r
            WHERE r.role = :role
              AND (:username IS NULL OR LOWER(su.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:name IS NULL OR LOWER(su.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:active IS NULL OR su.active = :active)
            """)
    Page<Long> findIdsByRoleAndFilters(@Param("role") String role,
            @Param("username") String username,
            @Param("name") String name,
            @Param("active") Boolean active,
            Pageable pageable);

    @Transactional(readOnly = true)
    List<SystemUser> findAllByIdIn(List<Long> ids);

}
