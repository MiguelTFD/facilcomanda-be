package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.TableService;
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
 * Autorización por rol de /api/tables: lectura para el personal de comandas y,
 * desde la feature 021 (roles.md decisión 5), también CAJERO (las vistas de
 * gestión de caja listan mesas); escritura ADMIN/SUPERADMIN y CAJERO.
 */
@WebMvcTest(TableController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class TableControllerAuthorizationTest {

    private static final String VALID_BODY =
            "{\"name\": \"Mesa 1\", \"state\": \"AVAILABLE\", \"chairs\": 4, \"floorId\": 1}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TableService tableService;

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getAllTables(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/tables").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getTableById(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/tables/1").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getTablesByFloorId(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/tables/floor/1").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,201", "ADMIN,201", "SUPERADMIN,201"})
    void createTable(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/tables").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void updateTable(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/tables/1").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,204", "ADMIN,204", "SUPERADMIN,204"})
    void deleteTable(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(delete("/api/tables/1").with(as(role))), expectedStatus);
    }
}
