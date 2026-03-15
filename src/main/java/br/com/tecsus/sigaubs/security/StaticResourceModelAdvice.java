package br.com.tecsus.sigaubs.security;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

@ControllerAdvice
public class StaticResourceModelAdvice {

    private final ResourceUrlProvider resourceUrlProvider;

    public StaticResourceModelAdvice(ResourceUrlProvider resourceUrlProvider) {
        this.resourceUrlProvider = resourceUrlProvider;
    }

    @ModelAttribute("res")
    public StaticResourceHelper res() {
        return new StaticResourceHelper(resourceUrlProvider);
    }
}
