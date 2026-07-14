package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.CategoryService;
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
 * Autorización por rol de /api/categories (feature 001): lectura para
 * MESERO/COCINERO/ADMIN/SUPERADMIN, escritura solo ADMIN/SUPERADMIN.
 */
@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class CategoryControllerAuthorizationTest {

    private static final String VALID_BODY = "{\"name\": \"Bebidas\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void getAllCategories(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/categories").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void getCategoryById(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/categories/1").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,201", "SUPERADMIN,201"})
    void createCategory(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/categories").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void updateCategory(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/categories/1").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,204", "SUPERADMIN,204"})
    void deleteCategory(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(delete("/api/categories/1").with(as(role))), expectedStatus);
    }
}
