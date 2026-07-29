package br.com.tecsus.sigaubs.controllers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TenantManagementLayoutTest {

    @Test
    void deveManterConteudoRolavelComTodosOsPaineisAbertos()
            throws IOException {
        String template = Files.readString(Path.of(
                "src/main/jte/tenantManagement/tenant_management.jte"));
        String header = Files.readString(Path.of(
                "src/main/jte/fragments/header.jte"));
        String footer = Files.readString(Path.of(
                "src/main/jte/fragments/footer.jte"));

        assertThat(template)
                .contains("h-dvh justify-between items-center overflow-hidden")
                .contains("h-full min-h-0 min-w-0 w-full overflow-hidden")
                .contains("flex-1 min-h-0 overflow-y-auto overflow-x-hidden")
                .contains("flex flex-col flex-shrink-0 mx-4")
                .contains("peer-checked:max-h-[3000px]");
        assertThat(template.indexOf("@template.fragments.footer()"))
                .as("footer deve pertencer ao fluxo rolável antes do fim da section")
                .isLessThan(template.indexOf("</section>"));
        assertThat(header).contains("flex-shrink-0");
        assertThat(footer).contains("flex-shrink-0");
    }
}
