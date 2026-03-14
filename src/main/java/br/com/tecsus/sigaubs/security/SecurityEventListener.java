package br.com.tecsus.sigaubs.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventListener implements ApplicationListener<AbstractAuthenticationFailureEvent> {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventListener.class);

    @Override
    public void onApplicationEvent(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getClass().getSimpleName();
        log.warn("Falha de autenticação — usuário: '{}', motivo: {}", username, reason);
    }
}
