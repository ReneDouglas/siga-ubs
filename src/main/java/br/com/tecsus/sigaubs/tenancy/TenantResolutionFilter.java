package br.com.tecsus.sigaubs.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantResolverService tenantResolverService;

    public TenantResolutionFilter(TenantResolverService tenantResolverService) {
        this.tenantResolverService = tenantResolverService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            var slug = tenantResolverService.resolveSlug(request.getHeader("X-Tenant-Slug"), request.getHeader("Host"));
            if (slug.isEmpty()) {
                if (isAllowedWithoutTenant(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                response.sendError(HttpStatus.NOT_FOUND.value());
                return;
            }

            TenantContext tenant = tenantResolverService.findActiveContextBySlug(slug.get()).orElse(null);
            if (tenant == null) {
                response.sendError(HttpStatus.NOT_FOUND.value());
                return;
            }

            TenantContextHolder.setTenant(tenant.id(), tenant.slug());
            MDC.put("tenant_id", String.valueOf(tenant.id()));
            MDC.put("tenant_slug", tenant.slug());
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
}
