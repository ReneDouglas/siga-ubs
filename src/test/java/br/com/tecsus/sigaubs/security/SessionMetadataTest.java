package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SessionMetadataTest {

    @Test
    void deveDescreverLocalidadeAproximadaRecebidaDoProxyConfiavel() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionMetadata.LOCATION_CITY_HEADER, "Afogados da Ingazeira");
        request.addHeader(SessionMetadata.LOCATION_REGION_CODE_HEADER, "pe");
        request.addHeader(SessionMetadata.LOCATION_COUNTRY_HEADER, "br");

        assertThat(SessionMetadata.describeLocation(
                request,
                SecurityProperties.SessionLocationSource.TRUSTED_PROXY))
                .isEqualTo("Afogados da Ingazeira, PE · BR");
    }

    @Test
    void deveDecodificarUtf8ESanitizarValoresDoCabecalho() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                SessionMetadata.LOCATION_CITY_HEADER,
                "SÃ£o Paulo\r\n<script>alert(1)</script>");
        request.addHeader(SessionMetadata.LOCATION_COUNTRY_HEADER, "BR");

        String result = SessionMetadata.describeLocation(
                request,
                SecurityProperties.SessionLocationSource.TRUSTED_PROXY);

        assertThat(result)
                .isEqualTo("São Paulo script alert(1) script · BR")
                .doesNotContain("\r", "\n", "<", ">");
    }

    @Test
    void deveTratarTorPaisDesconhecidoEAusenciaDeCabecalhos() {
        MockHttpServletRequest torRequest = new MockHttpServletRequest();
        torRequest.addHeader(SessionMetadata.LOCATION_COUNTRY_HEADER, "T1");
        assertThat(SessionMetadata.describeLocation(
                torRequest,
                SecurityProperties.SessionLocationSource.TRUSTED_PROXY))
                .isEqualTo("Rede Tor · localidade indisponível");

        MockHttpServletRequest unknownRequest = new MockHttpServletRequest();
        unknownRequest.addHeader(SessionMetadata.LOCATION_COUNTRY_HEADER, "XX");
        assertThat(SessionMetadata.describeLocation(
                unknownRequest,
                SecurityProperties.SessionLocationSource.TRUSTED_PROXY))
                .isEqualTo(SessionMetadata.LOCATION_UNAVAILABLE);

        assertThat(SessionMetadata.describeLocation(
                new MockHttpServletRequest(),
                SecurityProperties.SessionLocationSource.TRUSTED_PROXY))
                .isEqualTo(SessionMetadata.LOCATION_UNAVAILABLE);
    }

    @Test
    void deveIgnorarCabecalhosForaDoModoProxyConfiavel() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionMetadata.LOCATION_CITY_HEADER, "Local adulterado");
        request.addHeader(SessionMetadata.LOCATION_COUNTRY_HEADER, "BR");

        assertThat(SessionMetadata.describeLocation(
                request,
                SecurityProperties.SessionLocationSource.LOCAL))
                .isEqualTo(SessionMetadata.LOCAL_DEVELOPMENT_LOCATION);
        assertThat(SessionMetadata.describeLocation(
                request,
                SecurityProperties.SessionLocationSource.DISABLED))
                .isEqualTo(SessionMetadata.LOCATION_UNAVAILABLE);
    }
}
