package br.com.tecsus.sigaubs.security;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SecurityModelAdvice {

    @ModelAttribute("secHelper")
    public SecurityHelper secHelper() {
        return new SecurityHelper();
    }
}
