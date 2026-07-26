package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.SystemMaintenance;
import br.com.tecsus.sigaubs.repositories.SystemMaintenanceRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SystemMaintenanceService {

    private final SystemMaintenanceRepository systemMaintenanceRepository;

    public SystemMaintenanceService(SystemMaintenanceRepository systemMaintenanceRepository) {
        this.systemMaintenanceRepository = systemMaintenanceRepository;
    }

    @Cacheable("systemMaintenance")
    @Transactional(readOnly = true)
    public SystemMaintenance getCurrent() {
        return systemMaintenanceRepository.findById(SystemMaintenance.SINGLETON_ID)
                .orElseGet(SystemMaintenance::disabled);
    }

    public boolean isEnabled() {
        return getCurrent().isEnabled();
    }

    @CacheEvict(value = "systemMaintenance", allEntries = true)
    @Transactional
    public SystemMaintenance update(Boolean enabled,
            String message,
            LocalDateTime endDate,
            SystemUserDetails loggedUser) {

        SystemMaintenance maintenance = systemMaintenanceRepository.findById(SystemMaintenance.SINGLETON_ID)
                .orElseGet(SystemMaintenance::disabled);
        boolean shouldEnable = Boolean.TRUE.equals(enabled);
        maintenance.setEnabled(shouldEnable);
        maintenance.setMessage(normalizeBlank(message));
        maintenance.setEndDate(endDate);
        maintenance.setUpdateDate(LocalDateTime.now());
        maintenance.setUpdateUser(loggedUser.getUsername());

        if (shouldEnable && maintenance.getStartDate() == null) {
            maintenance.setStartDate(LocalDateTime.now());
        }
        if (!shouldEnable) {
            maintenance.setStartDate(null);
            maintenance.setEndDate(null);
        }

        return systemMaintenanceRepository.save(maintenance);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
