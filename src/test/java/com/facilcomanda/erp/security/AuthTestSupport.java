package com.facilcomanda.erp.security;

import com.facilcomanda.erp.model.enums.RoleName;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Soporte compartido para los tests de autorización por rol (feature 001).
 *
 * <p>Fabrica {@link CustomAuthentication} por {@link RoleName} y expone el
 * {@link RequestPostProcessor} que la inyecta en MockMvc, sin necesidad de
 * base de datos ni JWT real.</p>
 */
public final class AuthTestSupport {

    public static final Long ORG_ID = 1L;

    private AuthTestSupport() {
    }

    public static CustomAuthentication authenticationFor(RoleName role) {
        return new CustomAuthentication(
                role.name().toLowerCase() + "@facilcomanda.test",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
                ORG_ID);
    }

    public static RequestPostProcessor as(RoleName role) {
        return SecurityMockMvcRequestPostProcessors.authentication(authenticationFor(role));
    }

    /**
     * Verifica el estado esperado; si es 403, exige además el cuerpo de la
     * decisión D2: {@code {"message":"Acceso denegado"}}.
     */
    public static void assertAccess(ResultActions actions, int expectedStatus) throws Exception {
        actions.andExpect(status().is(expectedStatus));
        if (expectedStatus == 403) {
            actions.andExpect(jsonPath("$.message").value("Acceso denegado"));
        }
    }

    /**
     * Stubs de las dependencias que {@code SecurityConfig} exige por constructor
     * en un slice {@code @WebMvcTest}: {@code JwtService} para
     * {@code AuthenticationFilter} (sin header Authorization el filtro no lo usa)
     * y {@code EntityManager} para {@code TenantFilterEnabler} (con una
     * {@link Session} stubbeada para que {@code enableFilter("tenantFilter")}
     * no falle).
     */
    @TestConfiguration
    public static class SecurityFilterStubs {

        @Bean
        JwtService jwtService() {
            return Mockito.mock(JwtService.class);
        }

        @Bean
        EntityManager entityManager() {
            EntityManager entityManager = Mockito.mock(EntityManager.class);
            Session session = Mockito.mock(Session.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(entityManager.unwrap(Session.class)).thenReturn(session);
            return entityManager;
        }
    }
}
