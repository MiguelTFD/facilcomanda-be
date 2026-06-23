package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.ProductRequest;
import com.facilcomanda.erp.dto.ProductResponse;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return new ResponseEntity<>(productService.createProduct(request, orgId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(productService.getAllProducts(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(productService.getProductById(id, orgId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(productService.updateProduct(id, request, orgId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        productService.deleteProduct(id, orgId);
        return ResponseEntity.noContent().build();
    }
}
