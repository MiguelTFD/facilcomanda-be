package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.UserService;
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
 * Autorización por rol de /api/users (feature 001): CRUD completo solo para
 * ADMIN/SUPERADMIN. Incluye el criterio de aceptación 2: MESERO recibe 403
 * en POST /api/users.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class UserControllerAuthorizationTest {

    private static final String VALID_BODY =
            "{\"email\": \"nuevo@facilcomanda.test\", \"password\": \"secreta123\","
                    + " \"firstName\": \"Juan\", \"lastName\": \"Perez\", \"roleId\": 1}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void getAllUsers(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/users").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void getUserById(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/users/1").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,201", "SUPERADMIN,201"})
    void createUser(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/users").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void updateUser(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/users/1").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,204", "SUPERADMIN,204"})
    void deleteUser(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(delete("/api/users/1").with(as(role))), expectedStatus);
    }
}
