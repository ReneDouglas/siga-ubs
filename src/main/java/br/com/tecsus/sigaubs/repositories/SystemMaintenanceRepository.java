package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.SystemMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemMaintenanceRepository extends JpaRepository<SystemMaintenance, Long> {
}
