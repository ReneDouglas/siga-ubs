package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.services.PasswordPolicyService;
import br.com.tecsus.sigaubs.utils.DocumentValidationUtils;
import br.com.tecsus.sigaubs.utils.LogValueSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityCoreTest {

    @Test
    void timeoutsDeSessaoDiferenciamAdminGlobalDosUsuariosTenant() {
        SecurityProperties properties = new SecurityProperties();

        assertThat(properties.getSession().getAdminIdle())
                .isEqualTo(Duration.ofHours(2));
        assertThat(properties.getSession().getAdminAbsolute())
                .isEqualTo(Duration.ofHours(12));
        assertThat(properties.getSession().getTenantIdle())
                .isEqualTo(Duration.ofHours(12));
        assertThat(properties.getSession().getTenantAbsolute())
                .isEqualTo(Duration.ofHours(72));
    }

    @Test
    void reautenticacaoEVinculadaAoAtorAcaoObjetoPrazoEUsoUnico() {
        SecurityProperties properties = new SecurityProperties();
        properties.getSession().setReauthentication(Duration.ofMinutes(5));
        MutableClock clock = new MutableClock(Instant.parse("2026-07-29T12:00:00Z"));
        ReauthenticationService service = new ReauthenticationService(properties, clock);
        SystemUserDetails user = tenantUser(10L);
        MockHttpSession session = new MockHttpSession();

        service.issue(session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 100L);
        service.requireAndConsume(
                session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 100L);
        assertThatThrownBy(() -> service.requireAndConsume(
                session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 100L))
                .isInstanceOf(AccessDeniedException.class);

        service.issue(session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 100L);
        assertThatThrownBy(() -> service.requireAndConsume(
                session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 101L))
                .isInstanceOf(AccessDeniedException.class);

        service.issue(session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 100L);
        clock.advance(Duration.ofMinutes(6));
        assertThatThrownBy(() -> service.requireAndConsume(
                session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reautenticacaoRecusaEmissaoComEscopoInvalidoEOutroAtor() {
        SecurityProperties properties = new SecurityProperties();
        ReauthenticationService service = new ReauthenticationService(
                properties, Clock.fixed(Instant.parse("2026-07-29T12:00:00Z"), ZoneId.of("UTC")));
        MockHttpSession session = new MockHttpSession();
        SystemUserDetails user = tenantUser(10L);

        assertThatThrownBy(() -> service.issue(session, user, "OTHER", 100L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issue(
                session, user, ReauthenticationService.MANUAL_CONTEMPLATION, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issue(
                session, null, ReauthenticationService.MANUAL_CONTEMPLATION, 100L))
                .isInstanceOf(IllegalArgumentException.class);

        service.issue(session, user, ReauthenticationService.MANUAL_CONTEMPLATION, 100L);
        assertThatThrownBy(() -> service.requireAndConsume(
                session, tenantUser(11L), ReauthenticationService.MANUAL_CONTEMPLATION, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void politicaDeSenhaExigeOitoCaracteresELimitaBcrypt() {
        SecurityProperties properties = new SecurityProperties();
        PasswordPolicyService policy = new PasswordPolicyService(properties);

        assertThat(policy.validate("Forte#2026", "Forte#2026", true).sucesso()).isTrue();
        assertThat(policy.validate("1234567", "1234567", true).falhou()).isTrue();
        assertThat(policy.validate("12345678", "12345678", true).falhou()).isTrue();
        assertThat(policy.validate("Forte#2026", "outra", true).falhou()).isTrue();
        assertThat(DocumentValidationUtils.isAcceptablePassword("á".repeat(37), 8, 64)).isFalse();
    }

    @Test
    void documentosELogsSaoNormalizadosEValidadosSemInjecaoDeLinha() {
        assertThat(DocumentValidationUtils.isValidCpf("529.982.247-25")).isTrue();
        assertThat(DocumentValidationUtils.isValidCpf("111.111.111-11")).isFalse();
        assertThat(DocumentValidationUtils.isValidCns("174224524520048")).isTrue();
        assertThat(DocumentValidationUtils.isValidBrazilianPhone("(81) 99999-1234")).isTrue();
        assertThat(DocumentValidationUtils.digitsOnly("529.982.247-25")).isEqualTo("52998224725");

        String sanitized = LogValueSanitizer.sanitize("ok\r\nWARN forged\t\u0000");
        assertThat(sanitized).doesNotContain("\r", "\n", "\t", "\u0000");
        assertThat(LogValueSanitizer.sanitize("x".repeat(400))).hasSize(256);
    }

    @Test
    void escopoDeUbsNaoPodeSerAdulteradoEChaveDeSessaoEEstavel() {
        AuthorizationScopeService scope = new AuthorizationScopeService();
        SystemUserDetails localUser = new SystemUserDetails(
                20L,
                "atendente",
                "{noop}senha",
                List.of(new SimpleGrantedAuthority(Roles.ROLE_ATENDENTE.toString())),
                "Atendente",
                "atendente@example.com",
                true,
                7L,
                1L,
                "afogados");
        assertThat(scope.resolveAuthorizedBasicHealthUnit(localUser, 7L)).isEqualTo(7L);
        assertThatThrownBy(() -> scope.resolveAuthorizedBasicHealthUnit(localUser, 8L))
                .isInstanceOf(RuntimeException.class);
        assertThat(localUser.getSessionPrincipalKey()).isEqualTo("tenant:1:user:20");

        SystemUserDetails coordinator = tenantUser(10L);
        assertThat(scope.resolveAuthorizedBasicHealthUnit(coordinator, 8L)).isEqualTo(8L);

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                localUser, localUser.getPassword(), localUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            assertThat(new SecurityHelper().username()).isEqualTo("atendente");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private SystemUserDetails tenantUser(Long userId) {
        return new SystemUserDetails(
                userId,
                "sms",
                "{noop}senha",
                List.of(new SimpleGrantedAuthority(Roles.ROLE_SMS.toString())),
                "SMS",
                "sms@example.com",
                true,
                null,
                1L,
                "afogados");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
