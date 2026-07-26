package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.AdminUserSearchDTO;
import br.com.tecsus.sigaubs.dtos.SmsUserSearchDTO;
import br.com.tecsus.sigaubs.dtos.TenantSearchDTO;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.services.AdminSmsUserService;
import br.com.tecsus.sigaubs.services.AdminUserManagementService;
import br.com.tecsus.sigaubs.services.SystemMaintenanceService;
import br.com.tecsus.sigaubs.services.TenantManagementService;
import br.com.tecsus.sigaubs.services.TenantSessionService;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.admin;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSortingControllerTest {

    @Mock
    private AdminSmsUserService adminSmsUserService;
    @Mock
    private AdminUserManagementService adminUserManagementService;
    @Mock
    private TenantManagementService tenantManagementService;
    @Mock
    private SystemMaintenanceService systemMaintenanceService;
    @Mock
    private TenantSessionService tenantSessionService;

    @Test
    void deveNormalizarDirectionMinusculoSemUsarExcecao() {
        when(adminUserManagementService.findAdmins(any(AdminUserSearchDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<SystemAdmin>(List.of()));
        var controller = new AdminUserManagementController(adminUserManagementService);

        controller.getAdminUsersPage(model(), new AdminUserSearchDTO(), null, 0, 10,
                "username", "asc", true, admin());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminUserManagementService).findAdmins(any(AdminUserSearchDTO.class), pageableCaptor.capture());
        assertThat(directionFor(pageableCaptor.getValue(), "username")).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void deveUsarDirectionDescQuandoValorForInvalido() {
        when(tenantManagementService.findTenantsPaginated(any(TenantSearchDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<Tenant>(List.of()));
        when(tenantManagementService.getStatuses()).thenReturn(new br.com.tecsus.sigaubs.enums.TenantStatus[0]);
        var controller = new TenantManagementController(tenantManagementService, systemMaintenanceService,
                tenantSessionService);

        controller.getTenantManagementPage(model(), new TenantSearchDTO(), null, 0, 10,
                "slug", "invalid", true);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(tenantManagementService).findTenantsPaginated(any(TenantSearchDTO.class), pageableCaptor.capture());
        assertThat(directionFor(pageableCaptor.getValue(), "slug")).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void deveUsarDirectionDescQuandoValorForNulo() {
        when(adminSmsUserService.findTenant(1L)).thenReturn(TestDataFactory.tenant(1L, "afogados"));
        when(adminSmsUserService.findSmsUsers(eq(1L), any(SmsUserSearchDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<SystemUser>(List.of()));
        var controller = new AdminSmsUserController(adminSmsUserService);

        controller.getSmsUsersPage(1L, model(), new SmsUserSearchDTO(), null, 0, 10,
                "username", null, true);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminSmsUserService).findSmsUsers(eq(1L), any(SmsUserSearchDTO.class), pageableCaptor.capture());
        assertThat(directionFor(pageableCaptor.getValue(), "username")).isEqualTo(Sort.Direction.DESC);
    }

    private Sort.Direction directionFor(Pageable pageable, String property) {
        return pageable.getSort().getOrderFor(property).getDirection();
    }
}
