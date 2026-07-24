package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SystemUserServiceTenantTest {

    private final TenantRepository tenantRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final SystemUserRepository systemUserRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final SystemUserService systemUserService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    SystemUserServiceTenantTest(TenantRepository tenantRepository,
            SystemRoleRepository systemRoleRepository,
            SystemUserRepository systemUserRepository,
            SystemAdminRepository systemAdminRepository,
            SystemUserService systemUserService,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.systemUserRepository = systemUserRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.systemUserService = systemUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void deveCarregarUsuarioDoTenantAtualMesmoComUsernameRepetido() {
        Tenant afogados = createTenant("afogados", "Afogados");
        Tenant caruaru = createTenant("caruaru", "Caruaru");
        SystemRole roleUser = createRole(Roles.ROLE_USER);

        withTenant(afogados, () -> {
            systemUserRepository.saveAndFlush(systemUser("user", "Usuário Afogados", roleUser));
            return null;
        });
        withTenant(caruaru, () -> {
            systemUserRepository.saveAndFlush(systemUser("user", "Usuário Caruaru", roleUser));
            return null;
        });

        SystemUserDetails userAfogados = withTenant(afogados,
                () -> (SystemUserDetails) systemUserService.loadUserByUsername("user"));
        SystemUserDetails userCaruaru = withTenant(caruaru,
                () -> (SystemUserDetails) systemUserService.loadUserByUsername("user"));

        assertThat(userAfogados.getName()).isEqualTo("Usuário Afogados");
        assertThat(userAfogados.getTenantId()).isEqualTo(afogados.getId());
        assertThat(userAfogados.getTenantSlug()).isEqualTo("afogados");
        assertThat(userAfogados.getAuthorities()).extracting("authority").containsExactly(Roles.ROLE_USER.toString());

        assertThat(userCaruaru.getName()).isEqualTo("Usuário Caruaru");
        assertThat(userCaruaru.getTenantId()).isEqualTo(caruaru.getId());
        assertThat(userCaruaru.getTenantSlug()).isEqualTo("caruaru");
        assertThat(userCaruaru.getAuthorities()).extracting("authority").containsExactly(Roles.ROLE_USER.toString());
    }

    @Test
    void deveCarregarAdminGlobalNoTenantAtual() {
        Tenant afogados = createTenant("afogados", "Afogados");
        Tenant caruaru = createTenant("caruaru", "Caruaru");
        systemAdminRepository.saveAndFlush(systemAdmin("admin", "Administrador Global"));

        SystemUserDetails adminAfogados = withTenant(afogados,
                () -> (SystemUserDetails) systemUserService.loadUserByUsername("admin"));
        SystemUserDetails adminCaruaru = withTenant(caruaru,
                () -> (SystemUserDetails) systemUserService.loadUserByUsername("admin"));

        assertThat(adminAfogados.getAuthorities()).extracting("authority").containsExactly(Roles.ROLE_ADMIN.toString());
        assertThat(adminAfogados.getTenantId()).isEqualTo(afogados.getId());
        assertThat(adminAfogados.getTenantSlug()).isEqualTo("afogados");
        assertThat(adminAfogados.getBasicHealthUnitId()).isNull();

        assertThat(adminCaruaru.getAuthorities()).extracting("authority").containsExactly(Roles.ROLE_ADMIN.toString());
        assertThat(adminCaruaru.getTenantId()).isEqualTo(caruaru.getId());
        assertThat(adminCaruaru.getTenantSlug()).isEqualTo("caruaru");
        assertThat(adminCaruaru.getBasicHealthUnitId()).isNull();
    }

    private Tenant createTenant(String slug, String name) {
        Tenant tenant = new Tenant();
        tenant.setSlug(slug);
        tenant.setName(name);
        tenant.setDomain(slug + ".sigaubs.com.br");
        tenant.setStatus("ACTIVE");
        tenant.setCreationDate(LocalDateTime.now());
        tenant.setCreationUser("test");
        return tenantRepository.saveAndFlush(tenant);
    }

    private SystemRole createRole(Roles role) {
        SystemRole systemRole = new SystemRole();
        systemRole.setRole(role.toString());
        systemRole.setTitle(role.getDescription());
        systemRole.setDescription(role.getDescription());
        systemRole.setRoot(!role.getPermission());
        systemRole.setCreationDate(LocalDateTime.now());
        systemRole.setCreationUser("test");
        return systemRoleRepository.saveAndFlush(systemRole);
    }

    private SystemUser systemUser(String username, String name, SystemRole role) {
        SystemUser systemUser = new SystemUser();
        systemUser.setUsername(username);
        systemUser.setPassword(passwordEncoder.encode("123456"));
        systemUser.setName(name);
        systemUser.setEmail(username + "@example.com");
        systemUser.setActive(true);
        systemUser.setRoles(Set.of(role));
        systemUser.setCreationDate(LocalDateTime.now());
        systemUser.setCreationUser("test");
        return systemUser;
    }

    private SystemAdmin systemAdmin(String username, String name) {
        SystemAdmin systemAdmin = new SystemAdmin();
        systemAdmin.setUsername(username);
        systemAdmin.setPassword(passwordEncoder.encode("admin123"));
        systemAdmin.setName(name);
        systemAdmin.setEmail(username + "@example.com");
        systemAdmin.setActive(true);
        systemAdmin.setCreationDate(LocalDateTime.now());
        systemAdmin.setCreationUser("test");
        return systemAdmin;
    }

    private <T> T withTenant(Tenant tenant, Supplier<T> action) {
        TenantContextHolder.setTenant(tenant.getId(), tenant.getSlug());
        try {
            return action.get();
        } finally {
            TenantContextHolder.clear();
        }
    }
}
