package br.com.tecsus.sigaubs.tenancy;

import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.tenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantInfrastructureTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveControlarContextoDeTenant() {
        assertThat(TenantContextHolder.getCurrent()).isEmpty();
        TenantContextHolder.setTenant(10L, "afogados");

        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(10L);
        assertThat(TenantContextHolder.getRequiredTenantSlug()).isEqualTo("afogados");

        TenantContextHolder.clear();
        assertThatThrownBy(TenantContextHolder::getRequiredTenantId)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveResolverIdentificadorHibernateDoTenantAtual() {
        TenantIdentifierResolver resolver = new TenantIdentifierResolver();

        assertThat(resolver.resolveCurrentTenantIdentifier()).isZero();
        assertThat(resolver.validateExistingCurrentSessions()).isTrue();
        assertThat(resolver.isRoot(1L)).isFalse();

        TenantContextHolder.setTenant(22L, "caruaru");
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(22L);
    }

    @Test
    void deveBuscarApenasTenantAtivoPorSlugNormalizado() {
        TenantRepository repository = mock(TenantRepository.class);
        TenantResolverService resolver = new TenantResolverService(repository, "sigaubs.com.br");
        Tenant afogados = tenant(1L, "afogados");
        when(repository.findBySlugAndStatus("afogados", "ACTIVE")).thenReturn(Optional.of(afogados));

        assertThat(resolver.findActiveBySlug(" Afogados ")).contains(afogados);
        assertThat(resolver.findActiveBySlug("slug inválido")).isEmpty();
        assertThat(resolver.findActiveTenants()).isEmpty();
    }

    @Test
    void filtroDeResolucaoDevePopularELimparContexto() throws Exception {
        TenantResolverService resolver = mock(TenantResolverService.class);
        Tenant tenant = tenant(1L, "afogados");
        when(resolver.resolveSlug(null, "afogados.localhost")).thenReturn(Optional.of("afogados"));
        when(resolver.findActiveBySlug("afogados")).thenReturn(Optional.of(tenant));

        TenantResolutionFilter filter = new TenantResolutionFilter(resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Host", "afogados.localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(TenantContextHolder.getCurrent()).isEmpty();
    }

    @Test
    void filtroDeResolucaoDeveBloquearTenantInexistenteERootSemTenant() throws Exception {
        TenantResolverService resolver = mock(TenantResolverService.class);
        when(resolver.resolveSlug(null, "inexistente.localhost")).thenReturn(Optional.of("inexistente"));
        when(resolver.findActiveBySlug("inexistente")).thenReturn(Optional.empty());

        TenantResolutionFilter filter = new TenantResolutionFilter(resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Host", "inexistente.localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
        verify(chain, never()).doFilter(request, response);

        when(resolver.resolveSlug(null, "localhost")).thenReturn(Optional.empty());
        request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Host", "localhost");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void filtroDeResolucaoDevePermitirAssetsSemTenantEBloquearMismatch() throws Exception {
        TenantResolverService resolver = mock(TenantResolverService.class);
        when(resolver.resolveSlug(null, "localhost")).thenReturn(Optional.empty());
        when(resolver.resolveSlug("caruaru", "afogados.localhost"))
                .thenThrow(new TenantResolverService.TenantSlugMismatchException());

        TenantResolutionFilter filter = new TenantResolutionFilter(resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/css/app.css");
        request.addHeader("Host", "localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Host", "afogados.localhost");
        request.addHeader("X-Tenant-Slug", "caruaru");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void filtroDeSessaoDeveBloquearUsuarioDeOutroTenant() throws Exception {
        var principal = new SystemUserDetails(
                "user",
                "{noop}123456",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                "Usuário",
                "user@example.com",
                true,
                1L,
                1L,
                "afogados");
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        TenantContextHolder.setTenant(2L, "caruaru");

        TenantSessionValidationFilter filter = new TenantSessionValidationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }
}
