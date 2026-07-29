package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.time.Duration;

public class RoleAwareAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    private final SecurityProperties properties;
    private final boolean adminHost;

    public RoleAwareAuthenticationSuccessHandler(
            SecurityProperties properties, boolean adminHost, String defaultTargetUrl) {
        this.properties = properties;
        this.adminHost = adminHost;
        setDefaultTargetUrl(defaultTargetUrl);
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Duration idle = adminHost
                ? properties.getSession().getAdminIdle()
                : properties.getSession().getTenantIdle();
        request.getSession().setMaxInactiveInterval(Math.toIntExact(idle.toSeconds()));
        request.getSession().setAttribute(
                SessionMetadata.MANAGEMENT_ID_ATTRIBUTE,
                SessionMetadata.newManagementId());
        request.getSession().setAttribute(
                SessionMetadata.CLIENT_DESCRIPTION_ATTRIBUTE,
                SessionMetadata.describeClient(request));
        request.getSession().setAttribute(
                SessionMetadata.LOCATION_DESCRIPTION_ATTRIBUTE,
                SessionMetadata.describeLocation(
                        request,
                        properties.getSession().getLocationSource()));
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
