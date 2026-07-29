package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.enums.Roles;

import java.util.Set;
import java.util.stream.Collectors;

public record ActorContext(
        Long userId,
        String username,
        String displayName,
        Long tenantId,
        Long basicHealthUnitId,
        Set<String> authorities) {

    public static ActorContext from(SystemUserDetails user) {
        return new ActorContext(
                user.getUserId(),
                user.getLoginUsername(),
                user.getName(),
                user.getTenantId(),
                user.getBasicHealthUnitId(),
                user.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toUnmodifiableSet()));
    }

    public boolean hasRole(Roles role) {
        return authorities.contains(role.toString());
    }

    public boolean isGlobalAdmin() {
        return hasRole(Roles.ROLE_ADMIN);
    }

    public boolean isTenantCoordinator() {
        return hasRole(Roles.ROLE_SMS);
    }

    public boolean canAccessAllBasicHealthUnits() {
        return isGlobalAdmin() || isTenantCoordinator();
    }
}
