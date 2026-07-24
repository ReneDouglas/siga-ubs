package br.com.tecsus.sigaubs.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHelpersTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveVerificarRolesEUsuarioAutenticado() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "renato",
                "senha",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityHelper helper = new SecurityHelper();

        assertThat(helper.hasRole("ADMIN")).isTrue();
        assertThat(helper.hasRole("USER")).isFalse();
        assertThat(helper.hasAnyRole("USER", "ADMIN")).isTrue();
        assertThat(helper.username()).isEqualTo("renato");
    }

    @Test
    void deveRetornarValoresSegurosSemAutenticacao() {
        SecurityHelper helper = new SecurityHelper();

        assertThat(helper.hasRole("ADMIN")).isFalse();
        assertThat(helper.hasAnyRole("ADMIN", "SMS")).isFalse();
        assertThat(helper.username()).isEmpty();
    }

    @Test
    void deveExporTokenCsrfQuandoExiste() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        CsrfToken token = mock(CsrfToken.class);
        when(token.getParameterName()).thenReturn("_csrf");
        when(token.getToken()).thenReturn("abc");
        when(request.getAttribute(CsrfToken.class.getName())).thenReturn(token);

        CsrfHelper helper = new CsrfModelAdvice().csrf(request);

        assertThat(helper.parameterName()).isEqualTo("_csrf");
        assertThat(helper.token()).isEqualTo("abc");
    }

    @Test
    void deveExporCsrfVazioQuandoTokenNaoExiste() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        CsrfHelper helper = new CsrfModelAdvice().csrf(request);

        assertThat(helper.parameterName()).isEqualTo("_csrf");
        assertThat(helper.token()).isEmpty();
    }

    @Test
    void deveResolverUrlDeRecursoOuRetornarOriginal() {
        ResourceUrlProvider provider = new ResourceUrlProvider();
        provider.setHandlerMap(Map.of());

        StaticResourceHelper helper = new StaticResourceHelper(provider);

        assertThat(helper.url("/images/logo.svg")).isEqualTo("/images/logo.svg");
    }

    @Test
    void deveCriarHelpersPorModelAdvice() {
        assertThat(new SecurityModelAdvice().secHelper()).isInstanceOf(SecurityHelper.class);

        ResourceUrlProvider provider = new ResourceUrlProvider();
        provider.setHandlerMap(Map.of());
        assertThat(new StaticResourceModelAdvice(provider).res().url("/x")).isEqualTo("/x");
    }
}
