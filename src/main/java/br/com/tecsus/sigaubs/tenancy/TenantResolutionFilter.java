package br.com.tecsus.sigaubs.tenancy;

import br.com.tecsus.sigaubs.services.SystemMaintenanceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantResolverService tenantResolverService;
    private final SystemMaintenanceService systemMaintenanceService;

    @Autowired
    public TenantResolutionFilter(TenantResolverService tenantResolverService,
            SystemMaintenanceService systemMaintenanceService) {
        this.tenantResolverService = tenantResolverService;
        this.systemMaintenanceService = systemMaintenanceService;
    }

    public TenantResolutionFilter(TenantResolverService tenantResolverService) {
        this(tenantResolverService, null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            if (tenantResolverService.isAdminHost(request.getHeader("Host"))) {
                filterChain.doFilter(request, response);
                return;
            }

            var slug = tenantResolverService.resolveSlug(request.getHeader("X-Tenant-Slug"), request.getHeader("Host"));
            if (slug.isEmpty()) {
                if (isAllowedWithoutTenant(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                response.sendError(HttpStatus.NOT_FOUND.value());
                return;
            }

            TenantContext tenant = tenantResolverService.findContextBySlug(slug.get())
                    .or(() -> tenantResolverService.findActiveContextBySlug(slug.get()))
                    .orElse(null);
            if (tenant == null || tenant.isDisabled()) {
                response.sendError(HttpStatus.NOT_FOUND.value());
                return;
            }

            TenantContextHolder.setTenant(tenant.id(), tenant.slug());
            MDC.put("tenant_id", String.valueOf(tenant.id()));
            MDC.put("tenant_slug", tenant.slug());

            if (!isMaintenancePage(request)
                    && (isGlobalMaintenanceEnabled() || tenant.isMaintenance())) {
                forwardToMaintenance(request, response, tenant);
                return;
            }

            filterChain.doFilter(request, response);
        } catch (TenantResolverService.TenantSlugMismatchException e) {
            response.sendError(HttpStatus.FORBIDDEN.value());
        } finally {
            MDC.remove("tenant_id");
            MDC.remove("tenant_slug");
            TenantContextHolder.clear();
        }
    }

    private boolean isAllowedWithoutTenant(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico");
    }

    private boolean isMaintenancePage(HttpServletRequest request) {
        return request.getRequestURI().equals("/maintenance");
    }

    private void forwardToMaintenance(HttpServletRequest request,
            HttpServletResponse response,
            TenantContext tenant) throws ServletException, IOException {

        var globalMaintenance = systemMaintenanceService != null
                ? systemMaintenanceService.getCurrent()
                : br.com.tecsus.sigaubs.entities.SystemMaintenance.disabled();
        String message = globalMaintenance.isEnabled()
                ? globalMaintenance.getMessage()
                : tenant.maintenanceMessage();

        request.setAttribute("maintenanceMessage", message);
        request.setAttribute("maintenanceEndDate", globalMaintenance.isEnabled()
                ? globalMaintenance.getEndDate()
                : null);
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        request.getRequestDispatcher("/maintenance").forward(request, response);
    }

    private boolean isGlobalMaintenanceEnabled() {
        return systemMaintenanceService != null && systemMaintenanceService.isEnabled();
    }
}
