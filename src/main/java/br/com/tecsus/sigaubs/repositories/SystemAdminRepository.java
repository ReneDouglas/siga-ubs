package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SystemAdminRepository extends JpaRepository<SystemAdmin, Long> {

    @Transactional(readOnly = true)
    Optional<SystemAdmin> findByUsername(String username);
}
