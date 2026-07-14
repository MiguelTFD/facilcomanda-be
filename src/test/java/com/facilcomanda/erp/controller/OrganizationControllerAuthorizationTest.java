package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.OrganizationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.facilcomanda.erp.security.AuthTestSupport.as;
import static com.facilcomanda.erp.security.AuthTestSupport.assertAccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autorización por rol de /api/organizations (feature 001, D1): GET /my para
 * todos los autenticados (criterio 5, lo usa el perfil de usuario); PUT /my
 * solo ADMIN/SUPERADMIN; POST solo SUPERADMIN. Los roles fantasma
 * WEB_OWNER/ORG_MASTER quedan eliminados.
 */
@WebMvcTest(OrganizationController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class OrganizationControllerAuthorizationTest {

    private static final String VALID_BODY = "{\"name\": \"Mi Restaurante\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getMyOrganization(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/organizations/my").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void updateMyOrganization(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/organizations/my").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,403", "SUPERADMIN,201"})
    void createOrganization(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/organizations").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }
}
