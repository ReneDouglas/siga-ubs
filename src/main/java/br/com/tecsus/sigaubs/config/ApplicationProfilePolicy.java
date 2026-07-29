package br.com.tecsus.sigaubs.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class ApplicationProfilePolicy implements ApplicationRunner {

    private static final Set<String> ALLOWED_PROFILES = Set.of("dev", "prd", "test");

    private final Environment environment;

    public ApplicationProfilePolicy(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length != 1 || !ALLOWED_PROFILES.contains(profiles[0])) {
            throw new IllegalStateException(
                    "Exatamente um profile deve estar ativo: dev, prd ou test. Ativos: "
                            + Arrays.toString(profiles));
        }
    }
}
