package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.utils.DocumentValidationUtils;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private final SecurityProperties properties;

    public PasswordPolicyService(SecurityProperties properties) {
        this.properties = properties;
    }

    public ResultadoOperacao<Void> validate(String password, String confirmation, boolean required) {
        if (!required && (password == null || password.isBlank())) {
            return ResultadoOperacao.sucessoSemValor();
        }
        if (password == null || !password.equals(confirmation)) {
            return ResultadoOperacao.falha("As senhas não conferem.");
        }
        if (!DocumentValidationUtils.isAcceptablePassword(
                password,
                properties.getPassword().getMinimumLength(),
                properties.getPassword().getMaximumLength())) {
            return ResultadoOperacao.falha(
                    "A senha não atende ao comprimento mínimo ou está na lista de senhas proibidas.");
        }
        return ResultadoOperacao.sucessoSemValor();
    }
}
