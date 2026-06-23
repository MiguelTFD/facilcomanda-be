package com.facilcomanda.erp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class CustomAuthentication implements Authentication {

    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Long organizationId;
    private Object details;
    private boolean authenticated;

    public CustomAuthentication(String email, Collection<? extends GrantedAuthority> authorities, Long organizationId) {
        this.email = email;
        this.authorities = authorities;
        this.organizationId = organizationId;
        this.authenticated = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    @Override
    public Object getPrincipal() {
        return email;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return email;
    }

    public Long getOrganizationId() {
        return organizationId;
    }
}
