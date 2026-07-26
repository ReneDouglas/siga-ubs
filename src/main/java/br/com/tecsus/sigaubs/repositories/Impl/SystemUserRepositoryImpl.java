package br.com.tecsus.sigaubs.repositories.Impl;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.repositories.SystemUserRepositoryCustom;
import br.com.tecsus.sigaubs.utils.ValidationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

//@Repository
public class SystemUserRepositoryImpl implements SystemUserRepositoryCustom {

    private final EntityManager em;
    private ValidationUtils validationUtils;

    @Autowired
    public SystemUserRepositoryImpl(JpaContext jpaContext) {
        this.em = jpaContext.getEntityManagerByManagedType(SystemUser.class);
    }

    @Autowired
    public void setValidationUtils(ValidationUtils validationUtils) {
        this.validationUtils = validationUtils;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UBSsystemUserDTO> findSystemUsersNameByNameContains(String name) {

        String jpql = """
                        SELECT su.id, su.name, 'null', 'null' FROM SystemUser su
                        WHERE su.name LIKE CONCAT('%', :name, '%')
                        AND su.basicHealthUnit IS NULL
                        AND su.active IS TRUE
               """;

        TypedQuery<UBSsystemUserDTO> usersQuery = em.createQuery(jpql, UBSsystemUserDTO.class);
        usersQuery.setParameter("name", name);
        usersQuery.setMaxResults(5);

        return usersQuery.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SystemUser> findSystemUsersPaginated(SystemUser systemUser, Pageable page) {

        StringBuilder filters = new StringBuilder();
        boolean filteringByRole = validationUtils.attrIsNotNull(systemUser.getSelectedRoleId());

        filters.append("FROM SystemUser su ");
        if (filteringByRole) {
            filters.append("JOIN su.roles r ");
        }
        filters.append("WHERE 1 = 1 ");

        if (validationUtils.attrIsNotNull(systemUser.getUsername())) {
            filters.append("AND su.username = :username ");
        }
        if (validationUtils.attrIsNotNull(systemUser.getName())) {
            filters.append("AND su.name = :name ");
        }
        if (validationUtils.attrIsNotNull(systemUser.getBasicHealthUnit())) {
            filters.append("AND su.basicHealthUnit.id = :ubsId ");
        }
        if (filteringByRole) {
            filters.append("AND r.id = :roleId ");
        }
        if (validationUtils.attrIsNotNull(systemUser.getActive())) {
            filters.append("AND su.active = :active ");
        }

        String idsJpql = "SELECT su.id " + filters + "ORDER BY su.creationDate DESC ";

        TypedQuery<Long> systemUsersIdQuery = em.createQuery(idsJpql, Long.class);
        attachParameters(systemUsersIdQuery, systemUser);

        systemUsersIdQuery.setFirstResult(page.getPageNumber() * page.getPageSize());
        systemUsersIdQuery.setMaxResults(page.getPageSize());
        var systemUsersIds = systemUsersIdQuery.getResultList();

        Query count = em.createQuery("SELECT COUNT(su.id) " + filters);
        attachParameters(count, systemUser);
        long totalCountSystemUsers = (long) count.getSingleResult();

        if (systemUsersIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), page, totalCountSystemUsers);
        }

        TypedQuery<SystemUser> systemUsersQuery = em.createQuery("""
                                         SELECT su FROM SystemUser su
                                             LEFT JOIN FETCH su.roles
                                             LEFT JOIN FETCH su.basicHealthUnit
                                         WHERE su.id IN :ids
                                         ORDER BY su.creationDate DESC
                """, SystemUser.class);

        systemUsersQuery.setParameter("ids", systemUsersIds);
        List<SystemUser> systemUsers = systemUsersQuery.getResultList();

        return new PageImpl<>(systemUsers, page, totalCountSystemUsers);
    }

    private void attachParameters(Query query, SystemUser systemUser) {
        if (validationUtils.attrIsNotNull(systemUser.getUsername())) {
            query.setParameter("username", systemUser.getUsername());
        }
        if (validationUtils.attrIsNotNull(systemUser.getName())) {
            query.setParameter("name", systemUser.getName());
        }
        if (validationUtils.attrIsNotNull(systemUser.getBasicHealthUnit())) {
            query.setParameter("ubsId", systemUser.getBasicHealthUnit().getId());
        }
        if (validationUtils.attrIsNotNull(systemUser.getSelectedRoleId())) {
            query.setParameter("roleId", systemUser.getSelectedRoleId());
        }
        if (validationUtils.attrIsNotNull(systemUser.getActive())) {
            query.setParameter("active", systemUser.getActive());
        }
    }
}
