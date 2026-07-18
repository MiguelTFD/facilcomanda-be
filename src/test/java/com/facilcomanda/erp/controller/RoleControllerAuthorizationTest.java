package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.RoleService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autorización por rol de /api/roles: CRUD completo (GET incluido) para
 * ADMIN/SUPERADMIN y, desde la feature 021 (roles.md decisión 5), también
 * CAJERO (gestión operativa). MESERO/COCINERO reciben 403.
 */
@WebMvcTest(RoleController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class RoleControllerAuthorizationTest {

    private static final String VALID_BODY = "{\"name\": \"MESERO\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getAllRoles(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/roles").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getRoleById(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/roles/1").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,201", "ADMIN,201", "SUPERADMIN,201"})
    void createRole(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/roles").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void updateRole(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/roles/1").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,204", "ADMIN,204", "SUPERADMIN,204"})
    void deleteRole(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(delete("/api/roles/1").with(as(role))), expectedStatus);
    }
}
