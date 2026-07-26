package br.com.tecsus.sigaubs.tenancy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantResolverServiceTest {

    private final TenantResolverService tenantResolverService = new TenantResolverService(
            null,
            "sigaubs.com.br");

    @Test
    void deveResolverTenantPeloSubdominioDeProducao() {
        var slug = tenantResolverService.resolveSlug(null, "afogados.sigaubs.com.br");

        assertThat(slug.slug()).contains("afogados");
        assertThat(slug.mismatch()).isFalse();
    }

    @Test
    void deveResolverTenantPeloSubdominioLocalhost() {
        var slug = tenantResolverService.resolveSlug(null, "caruaru.localhost:8080");

        assertThat(slug.slug()).contains("caruaru");
        assertThat(slug.mismatch()).isFalse();
    }

    @Test
    void deveRetornarVazioParaDominioRaiz() {
        assertThat(tenantResolverService.resolveSlug(null, "sigaubs.com.br").slug()).isEmpty();
        assertThat(tenantResolverService.resolveSlug(null, "localhost:8080").slug()).isEmpty();
    }

    @Test
    void deveAceitarHeaderQuandoHostNaoTemTenant() {
        var slug = tenantResolverService.resolveSlug("afogados", "localhost:8080");

        assertThat(slug.slug()).contains("afogados");
        assertThat(slug.mismatch()).isFalse();
    }

    @Test
    void deveBloquearQuandoHeaderDivergeDoSubdominio() {
        var slug = tenantResolverService.resolveSlug("caruaru", "afogados.localhost");

        assertThat(slug.slug()).isEmpty();
        assertThat(slug.mismatch()).isTrue();
    }
}
