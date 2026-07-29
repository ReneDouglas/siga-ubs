package br.com.tecsus.sigaubs.security.config;

import br.com.tecsus.sigaubs.services.SystemUserService;
import br.com.tecsus.sigaubs.services.AdminUserDetailsService;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.tenancy.TenantResolutionFilter;
import br.com.tecsus.sigaubs.tenancy.TenantResolverService;
import br.com.tecsus.sigaubs.tenancy.TenantSessionValidationFilter;
import br.com.tecsus.sigaubs.security.SessionAbsoluteTimeoutFilter;
import br.com.tecsus.sigaubs.security.RoleAwareAuthenticationSuccessHandler;
import br.com.tecsus.sigaubs.config.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import java.util.HashMap;
import java.util.Map;

import static br.com.tecsus.sigaubs.security.UrlPatternConfig.PRIVATE_MATCHERS;
import static br.com.tecsus.sigaubs.security.UrlPatternConfig.PUBLIC_MATCHERS;
import static org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive.COOKIES;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final String SERVLET_SESSION_COOKIE = "JSESSIONID";
    private static final String SPRING_SESSION_COOKIE = "SESSION";
    private static final String APPLICATION_SESSION_COOKIE = "SIGAUBS_SESSION";
    private static final String SECURE_APPLICATION_SESSION_COOKIE =
            "__Host-SIGAUBS_SESSION";

    private final TenantResolutionFilter tenantResolutionFilter;
    private final TenantSessionValidationFilter tenantSessionValidationFilter;
    private final TenantResolverService tenantResolverService;
    private final SessionAbsoluteTimeoutFilter sessionAbsoluteTimeoutFilter;
    private final SecurityProperties securityProperties;

    @Autowired
    public WebSecurityConfig(TenantResolutionFilter tenantResolutionFilter,
            TenantSessionValidationFilter tenantSessionValidationFilter,
            TenantResolverService tenantResolverService,
            SessionAbsoluteTimeoutFilter sessionAbsoluteTimeoutFilter,
            SecurityProperties securityProperties) {
        this.tenantResolutionFilter = tenantResolutionFilter;
        this.tenantSessionValidationFilter = tenantSessionValidationFilter;
        this.tenantResolverService = tenantResolverService;
        this.sessionAbsoluteTimeoutFilter = sessionAbsoluteTimeoutFilter;
        this.securityProperties = securityProperties;
    }

    // authorization
    @Bean
    @Order(0)
    public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**");
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
            AdminUserDetailsService adminUserDetailsService,
            SessionRegistry sessionRegistry) throws Exception {

        http.securityMatcher(request ->
                tenantResolverService.isAdminHost(request.getHeader(HttpHeaders.HOST)));
        http.authorizeHttpRequests(authConfig -> {
            authConfig.requestMatchers(
                    "/css/**",
                    "/images/**",
                    "/js/**",
                    "/vendor/**",
                    "/admin/login",
                    "/admin/login-error",
                    "/error",
                    "/expired",
                    "/maintenance",
                    "/webjars/**",
                    "/favicon.ico",
                    "/actuator/health").permitAll();
            authConfig.requestMatchers("/admin/**")
                    .hasAuthority(Roles.ROLE_ADMIN.name());
            authConfig.anyRequest().denyAll();
        });
        http.formLogin(login -> {
            login.loginPage("/admin/login");
            login.loginProcessingUrl("/admin/login");
            login.failureUrl("/admin/login-error");
            login.successHandler(new RoleAwareAuthenticationSuccessHandler(
                    securityProperties, true, "/admin/tenant-management"));
        });
        http.logout(logout -> {
            logout.logoutUrl("/admin/logout");
            logout.logoutSuccessUrl("/admin/login");
            logout.permitAll();
            logout.addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(COOKIES)));
            logout.clearAuthentication(true);
            logout.deleteCookies(
                    SERVLET_SESSION_COOKIE,
                    SPRING_SESSION_COOKIE,
                    APPLICATION_SESSION_COOKIE,
                    SECURE_APPLICATION_SESSION_COOKIE);
            logout.invalidateHttpSession(true);
        });
        http.csrf(Customizer.withDefaults());
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> {
            session.sessionFixation(fixation -> fixation.newSession());
            session.sessionConcurrency(concurrency -> {
                concurrency.maximumSessions(
                                securityProperties.getSession().getMaximumConcurrentSessions())
                        .expiredUrl("/expired")
                        .maxSessionsPreventsLogin(true)
                        .sessionRegistry(sessionRegistry);
            });
        });
        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
        );
        http.authenticationProvider(adminAuthenticationProvider(adminUserDetailsService));
        http.addFilterAfter(sessionAbsoluteTimeoutFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain tenantSecurityFilterChain(HttpSecurity http,
            SystemUserService systemUserService,
            SessionRegistry sessionRegistry) throws Exception {

        http.authorizeHttpRequests(authConfig -> {
            authConfig.requestMatchers(PUBLIC_MATCHERS).permitAll();
            authConfig.requestMatchers("/actuator/health").permitAll();
            authConfig.requestMatchers("/actuator/**")
                    .hasAuthority(Roles.ROLE_ADMIN.name());
            authConfig.requestMatchers("/admin/**").denyAll();
            authConfig.requestMatchers(PRIVATE_MATCHERS).authenticated();
            authConfig.anyRequest().authenticated();
        });
        http.formLogin(login -> {
            login.loginPage("/login");
            login.failureUrl("/login-error");
            login.successHandler(new RoleAwareAuthenticationSuccessHandler(
                    securityProperties, false, "/"));
        });
        http.logout(logout -> {
            logout.logoutUrl("/logout");
            logout.logoutSuccessUrl("/login");
            logout.permitAll();
            logout.addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(COOKIES)));
            logout.clearAuthentication(true);
            logout.deleteCookies(
                    SERVLET_SESSION_COOKIE,
                    SPRING_SESSION_COOKIE,
                    APPLICATION_SESSION_COOKIE,
                    SECURE_APPLICATION_SESSION_COOKIE);
            logout.invalidateHttpSession(true);
        });
        http.csrf(Customizer.withDefaults());
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> {
            session.sessionFixation(fixation -> fixation.newSession());
            session.sessionConcurrency(concurrency -> {
                concurrency.maximumSessions(
                                securityProperties.getSession().getMaximumConcurrentSessions())
                        .expiredUrl("/expired")
                        .maxSessionsPreventsLogin(true)
                        .sessionRegistry(sessionRegistry);
            });
        });
        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
        );
        http.authenticationProvider(tenantAuthenticationProvider(systemUserService));
        http.addFilterBefore(tenantResolutionFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(tenantSessionValidationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(sessionAbsoluteTimeoutFilter, TenantSessionValidationFilter.class);

        return http.build();
    }

    // authentication
    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bcrypt =
                new BCryptPasswordEncoder(securityProperties.getPassword().getBcryptStrength());
        Map<String, org.springframework.security.crypto.password.PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", bcrypt);
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        encoder.setDefaultPasswordEncoderForMatches(bcrypt);
        return encoder;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRegistry sessionRegistry(JdbcIndexedSessionRepository sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
    }

    private DaoAuthenticationProvider tenantAuthenticationProvider(SystemUserService systemUserService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(systemUserService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsPasswordService(systemUserService);
        return authProvider;
    }

    private DaoAuthenticationProvider adminAuthenticationProvider(AdminUserDetailsService adminUserDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(adminUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsPasswordService(adminUserDetailsService);
        return authProvider;
    }

}
