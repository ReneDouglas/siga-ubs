package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WebSecurityLogoutTest {

    private static final String HOST = "logout-afogados.sigaubs.com.br";
    private static final String SLUG = "logout-afogados";
    private static final String USERNAME = "logout-user";
    private static final String PASSWORD = "123456";

    private final MockMvc mockMvc;
    private final TenantRepository tenantRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final SystemUserRepository systemUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    WebSecurityLogoutTest(WebApplicationContext webApplicationContext,
            TenantRepository tenantRepository,
            SystemRoleRepository systemRoleRepository,
            SystemUserRepository systemUserRepository,
            PasswordEncoder passwordEncoder) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        this.tenantRepository = tenantRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.systemUserRepository = systemUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void deveInvalidarSessaoNoLogoutComTenant() throws Exception {
        Tenant tenant = createTenant();
        SystemRole roleUser = createRole();
        createSystemUser(tenant, roleUser);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .header("Host", HOST)
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.isInvalid()).isFalse();

        mockMvc.perform(post("/logout")
                        .header("Host", HOST)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(session.isInvalid()).isTrue();
    }

    private Tenant createTenant() {
        return tenantRepository.findBySlugAndStatus(SLUG, "ACTIVE")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setSlug(SLUG);
                    tenant.setName("Logout Afogados");
                    tenant.setDomain(HOST);
                    tenant.setStatus("ACTIVE");
                    tenant.setCreationDate(LocalDateTime.now());
                    tenant.setCreationUser("test");
                    return tenantRepository.saveAndFlush(tenant);
                });
    }

    private SystemRole createRole() {
        return systemRoleRepository.findByRole(Roles.ROLE_USER.toString())
                .orElseGet(() -> {
                    SystemRole systemRole = new SystemRole();
                    systemRole.setRole(Roles.ROLE_USER.toString());
                    systemRole.setTitle(Roles.ROLE_USER.getDescription());
                    systemRole.setDescription(Roles.ROLE_USER.getDescription());
                    systemRole.setRoot(false);
                    systemRole.setCreationDate(LocalDateTime.now());
                    systemRole.setCreationUser("test");
                    return systemRoleRepository.saveAndFlush(systemRole);
                });
    }

    private void createSystemUser(Tenant tenant, SystemRole roleUser) {
        TenantContextHolder.setTenant(tenant.getId(), tenant.getSlug());
        try {
            if (systemUserRepository.findByUsername(USERNAME).isPresent()) {
                return;
            }

            SystemUser systemUser = new SystemUser();
            systemUser.setUsername(USERNAME);
            systemUser.setPassword(passwordEncoder.encode(PASSWORD));
            systemUser.setName("Usuário Logout");
            systemUser.setEmail("logout-user@example.com");
            systemUser.setActive(true);
            systemUser.setRoles(Set.of(roleUser));
            systemUser.setCreationDate(LocalDateTime.now());
            systemUser.setCreationUser("test");
            systemUserRepository.saveAndFlush(systemUser);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
