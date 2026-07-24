package br.com.tecsus.sigaubs.security.config;

import br.com.tecsus.sigaubs.services.SystemUserService;
import br.com.tecsus.sigaubs.tenancy.TenantResolutionFilter;
import br.com.tecsus.sigaubs.tenancy.TenantSessionValidationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static br.com.tecsus.sigaubs.security.UrlPatternConfig.PRIVATE_MATCHERS;
import static br.com.tecsus.sigaubs.security.UrlPatternConfig.PUBLIC_MATCHERS;
import static org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive.COOKIES;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final TenantResolutionFilter tenantResolutionFilter;
    private final TenantSessionValidationFilter tenantSessionValidationFilter;

    @Autowired
    public WebSecurityConfig(TenantResolutionFilter tenantResolutionFilter,
            TenantSessionValidationFilter tenantSessionValidationFilter) {
        this.tenantResolutionFilter = tenantResolutionFilter;
        this.tenantSessionValidationFilter = tenantSessionValidationFilter;
    }

    // authorization
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(authConfig -> {
            authConfig.requestMatchers(PUBLIC_MATCHERS).permitAll();
            authConfig.requestMatchers("/actuator/health").permitAll();
            authConfig.requestMatchers("/actuator/**").hasRole("ADMIN");
            authConfig.requestMatchers(PRIVATE_MATCHERS).authenticated();
            authConfig.anyRequest().authenticated();
        });
        http.formLogin(login -> {
            login.loginPage("/login");
            login.failureUrl("/login-error");
            login.defaultSuccessUrl("/", true);
        });
        http.logout(logout -> {
            logout.logoutUrl("/logout");
            logout.logoutSuccessUrl("/login");
            logout.permitAll();
            logout.addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(COOKIES)));
            logout.clearAuthentication(true);
            logout.deleteCookies("JSESSIONID");
            logout.invalidateHttpSession(true);
        });
        http.csrf(Customizer.withDefaults());
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> {
            session.sessionConcurrency(concurrency -> {
                concurrency.maximumSessions(3).expiredUrl("/expired").maxSessionsPreventsLogin(true);
            });
        });
        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
        );
        http.addFilterBefore(tenantResolutionFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(tenantSessionValidationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // authentication
    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder encoder = (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // Senhas legadas sem prefixo {id} são tratadas como BCrypt
        encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return encoder;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // Método importante para 'fazer enxergar' o userDetailsService
    @Bean
    public AuthenticationManager authenticationManager(SystemUserService systemUserService){
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(systemUserService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();
        configuration.setAllowedMethods(List.of("POST", "GET", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "X-Requested-With", "X-Tenant-Slug"));
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Content-Type");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
