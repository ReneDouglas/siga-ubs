package br.com.tecsus.sigaubs.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CsrfModelAdvice {

    @ModelAttribute("csrf")
    public CsrfHelper csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) return new CsrfHelper("_csrf", "");
        return new CsrfHelper(token.getParameterName(), token.getToken());
    }
}
