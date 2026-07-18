package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.ProductService;
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
 * Autorización por rol de /api/products: lectura para el personal de comandas y,
 * desde la feature 021 (roles.md decisión 5), también CAJERO; escritura
 * ADMIN/SUPERADMIN y CAJERO (gestión operativa). MESERO/COCINERO reciben 403 en
 * escritura.
 */
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class ProductControllerAuthorizationTest {

    private static final String VALID_BODY =
            "{\"name\": \"Pizza\", \"stock\": 10, \"unitPrice\": 25.50}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getAllProducts(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/products").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getProductById(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/products/1").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,201", "ADMIN,201", "SUPERADMIN,201"})
    void createProduct(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/products").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void updateProduct(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/products/1").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,204", "ADMIN,204", "SUPERADMIN,204"})
    void deleteProduct(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(delete("/api/products/1").with(as(role))), expectedStatus);
    }
}
