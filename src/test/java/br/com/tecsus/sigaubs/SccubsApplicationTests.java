package br.com.tecsus.sigaubs;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer conexão com banco de dados MySQL. Executar apenas em ambiente com banco disponível.")
class SccubsApplicationTests {

    @Test
    void contextLoads() {
    }

}
