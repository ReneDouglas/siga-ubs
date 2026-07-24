package br.com.tecsus.sigaubs.tenancy;

import br.com.tecsus.sigaubs.security.SystemUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantSessionValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var currentTenantId = TenantContextHolder.getCurrentTenantId().orElse(null);

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SystemUserDetails loggedUser
                && currentTenantId != null
                && !currentTenantId.equals(loggedUser.getTenantId())) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
