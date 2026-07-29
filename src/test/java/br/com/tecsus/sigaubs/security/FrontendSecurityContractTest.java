package br.com.tecsus.sigaubs.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendSecurityContractTest {

    private static final Pattern INLINE_SCRIPT =
            Pattern.compile("<script(?![^>]*\\bsrc\\s*=)[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_HANDLER =
            Pattern.compile("\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTERNAL_BROWSER_RESOURCE = Pattern.compile(
            "(?:src|href)\\s*=\\s*[\"']https?://", Pattern.CASE_INSENSITIVE);

    @Test
    void templatesNaoPossuemJavascriptInlineOuRecursosExternos() throws IOException {
        Path templateRoot = Path.of("src/main/jte");
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(templateRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".jte")).toList()) {
                String source = Files.readString(file);
                check(violations, file, source, INLINE_SCRIPT, "script inline");
                check(violations, file, source, INLINE_HANDLER, "handler inline");
                check(violations, file, source, EXTERNAL_BROWSER_RESOURCE, "recurso externo");
                if (source.contains("hx-on") || source.contains("js:")) {
                    violations.add(file + ": atributo HTMX executável");
                }
            }
        }

        assertThat(violations).as("violações de XSS/cadeia de suprimentos").isEmpty();
    }

    @Test
    void javascriptDaAplicacaoNaoUsaSinksHtmlPerigosos() throws IOException {
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(Path.of("src/main/resources/static/js"))) {
            for (Path file : files
                    .filter(path -> path.toString().endsWith(".js"))
                    .filter(path -> !path.getFileName().toString().endsWith(".min.js"))
                    .toList()) {
                String source = Files.readString(file);
                if (source.contains(".innerHTML")
                        || source.contains(".outerHTML")
                        || source.contains("insertAdjacentHTML")
                        || source.contains("document.write")) {
                    violations.add(file.toString());
                }
            }
        }
        assertThat(violations).as("sinks HTML no código próprio").isEmpty();
    }

    @Test
    void apexChartsUsaFolhaDeEstilosLocalCompativelComCsp() throws IOException {
        String head = Files.readString(Path.of("src/main/jte/fragments/head.jte"));
        String apexStyles = Files.readString(
                Path.of("src/main/resources/static/css/apexcharts-3.54.1.css"));

        assertThat(head).contains("${res.url(\"/css/apexcharts-3.54.1.css\")}");
        assertThat(apexStyles).contains(".apexcharts-canvas", ".apexcharts-legend");
    }

    private void check(List<String> violations, Path file, String source, Pattern pattern, String label) {
        if (pattern.matcher(source).find()) {
            violations.add(file + ": " + label);
        }
    }
}
