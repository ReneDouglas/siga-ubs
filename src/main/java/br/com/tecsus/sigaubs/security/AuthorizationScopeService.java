package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.entities.TenantScopedEntity;
import br.com.tecsus.sigaubs.exceptions.ForbiddenOperationException;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthorizationScopeService {

    public ActorContext currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SystemUserDetails user)) {
            throw new ForbiddenOperationException("Usuário autenticado não encontrado.");
        }
        return ActorContext.from(user);
    }

    public void requireCurrentTenant(TenantScopedEntity entity) {
        if (entity == null || !Objects.equals(entity.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw new ForbiddenOperationException("Objeto fora do tenant autenticado.");
        }
    }

    public Long resolveAuthorizedBasicHealthUnit(SystemUserDetails user, Long requestedBasicHealthUnitId) {
        ActorContext actor = ActorContext.from(user);
        if (actor.canAccessAllBasicHealthUnits()) {
            if (requestedBasicHealthUnitId == null) {
                throw new IllegalArgumentException("UBS obrigatória.");
            }
            return requestedBasicHealthUnitId;
        }

        if (actor.basicHealthUnitId() == null) {
            throw new ForbiddenOperationException("Usuário sem UBS vinculada.");
        }
        if (requestedBasicHealthUnitId != null
                && !Objects.equals(actor.basicHealthUnitId(), requestedBasicHealthUnitId)) {
            throw new ForbiddenOperationException("UBS fora do escopo do usuário.");
        }
        return actor.basicHealthUnitId();
    }

    public void requireBasicHealthUnit(SystemUserDetails user, Long objectBasicHealthUnitId) {
        ActorContext actor = ActorContext.from(user);
        if (!actor.canAccessAllBasicHealthUnits()
                && !Objects.equals(actor.basicHealthUnitId(), objectBasicHealthUnitId)) {
            throw new ForbiddenOperationException("Objeto fora da UBS autorizada.");
        }
    }
}
