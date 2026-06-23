package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.CategoryRequest;
import com.facilcomanda.erp.dto.CategoryResponse;
import com.facilcomanda.erp.model.Category;
import com.facilcomanda.erp.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request, Long organizationId) {
        Category category = new Category();
        category.setOrganizationId(organizationId);
        mapRequestToCategory(request, category, organizationId);

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories(Long organizationId) {
        return categoryRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id, Long organizationId) {
        Category category = fetchCategory(id, organizationId);
        return mapToResponse(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request, Long organizationId) {
        Category category = fetchCategory(id, organizationId);
        mapRequestToCategory(request, category, organizationId);

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    public void deleteCategory(Long id, Long organizationId) {
        Category category = fetchCategory(id, organizationId);
        categoryRepository.delete(category);
    }

    private Category fetchCategory(Long id, Long organizationId) {
        return categoryRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Category not found or unauthorized"));
    }

    private void mapRequestToCategory(CategoryRequest request, Category category, Long organizationId) {
        category.setName(request.name());
        category.setDescription(request.description());

        if (request.parentCategoryId() != null) {
            Category parentCategory = categoryRepository.findByIdAndOrganizationId(request.parentCategoryId(), organizationId)
                    .orElseThrow(() -> new RuntimeException("Parent category not found or unauthorized"));
            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null);
        }
    }

    private CategoryResponse mapToResponse(Category category) {
        Long parentId = category.getParentCategory() != null ? category.getParentCategory().getId() : null;
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                parentId
        );
    }
}
