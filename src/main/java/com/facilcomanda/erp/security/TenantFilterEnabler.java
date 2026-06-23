package com.facilcomanda.erp.security;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilterEnabler extends OncePerRequestFilter {

    private final EntityManager entityManager;

    public TenantFilterEnabler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof CustomAuthentication) {
            CustomAuthentication customAuth = (CustomAuthentication) auth;
            Long organizationId = customAuth.getOrganizationId();

            if (organizationId != null) {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("organizationId", organizationId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
