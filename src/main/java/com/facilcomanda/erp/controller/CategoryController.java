package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.CategoryRequest;
import com.facilcomanda.erp.dto.CategoryResponse;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return new ResponseEntity<>(categoryService.createCategory(request, orgId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(categoryService.getAllCategories(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(categoryService.getCategoryById(id, orgId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(categoryService.updateCategory(id, request, orgId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        categoryService.deleteCategory(id, orgId);
        return ResponseEntity.noContent().build();
    }
}
