package br.com.tecsus.sigaubs.security;

import org.springframework.web.servlet.resource.ResourceUrlProvider;

public class StaticResourceHelper {

    private final ResourceUrlProvider urlProvider;

    public StaticResourceHelper(ResourceUrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    public String url(String path) {
        String resolved = urlProvider.getForLookupPath(path);
        return resolved != null ? resolved : path;
    }
}
