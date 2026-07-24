package br.com.tecsus.sigaubs.tenancy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantResolverServiceTest {

    private final TenantResolverService tenantResolverService = new TenantResolverService(
            null,
            "sigaubs.com.br");

    @Test
    void deveResolverTenantPeloSubdominioDeProducao() {
        var slug = tenantResolverService.resolveSlug(null, "afogados.sigaubs.com.br");

        assertThat(slug).contains("afogados");
    }

    @Test
    void deveResolverTenantPeloSubdominioLocalhost() {
        var slug = tenantResolverService.resolveSlug(null, "caruaru.localhost:8080");

        assertThat(slug).contains("caruaru");
    }

    @Test
    void deveRetornarVazioParaDominioRaiz() {
        assertThat(tenantResolverService.resolveSlug(null, "sigaubs.com.br")).isEmpty();
        assertThat(tenantResolverService.resolveSlug(null, "localhost:8080")).isEmpty();
    }

    @Test
    void deveAceitarHeaderQuandoHostNaoTemTenant() {
        var slug = tenantResolverService.resolveSlug("afogados", "localhost:8080");

        assertThat(slug).contains("afogados");
    }

    @Test
    void deveBloquearQuandoHeaderDivergeDoSubdominio() {
        assertThatThrownBy(() -> tenantResolverService.resolveSlug("caruaru", "afogados.localhost"))
                .isInstanceOf(TenantResolverService.TenantSlugMismatchException.class);
    }
}
