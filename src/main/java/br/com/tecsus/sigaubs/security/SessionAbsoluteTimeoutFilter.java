package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

@Component
public class SessionAbsoluteTimeoutFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;
    private final Clock clock;

    @Autowired
    public SessionAbsoluteTimeoutFilter(SecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SessionAbsoluteTimeoutFilter(SecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);
        if (session != null
                && authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SystemUserDetails user) {
            boolean admin = user.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            Duration maximumLifetime = admin
                    ? properties.getSession().getAdminAbsolute()
                    : properties.getSession().getTenantAbsolute();
            if (clock.millis() >= session.getCreationTime() + maximumLifetime.toMillis()) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(admin && user.getTenantId() == null ? "/admin/login" : "/login");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
