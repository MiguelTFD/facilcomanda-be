package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.AuthService;
import com.facilcomanda.erp.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio 6 (feature 001): peticiones sin token reciben 401 (no 403) en
 * /api/**, y login/register siguen siendo públicos.
 */
@WebMvcTest({AuthController.class, ProductController.class})
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class PublicAndUnauthenticatedAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private ProductService productService;

    @Test
    void getSinTokenRecibe401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void escrituraSinTokenRecibe401() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Pizza\", \"stock\": 10, \"unitPrice\": 25.50}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEsPublico() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"user@facilcomanda.test\", \"password\": \"secreta123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void registerEsPublico() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"user@facilcomanda.test\", \"password\": \"secreta123\","
                                + " \"firstName\": \"Juan\", \"lastName\": \"Perez\","
                                + " \"roleId\": 1, \"organizationId\": 1}"))
                .andExpect(status().isCreated());
    }
}
