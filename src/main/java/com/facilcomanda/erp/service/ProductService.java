package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.ProductRequest;
import com.facilcomanda.erp.dto.ProductResponse;
import com.facilcomanda.erp.model.Category;
import com.facilcomanda.erp.model.Product;
import com.facilcomanda.erp.repository.CategoryRepository;
import com.facilcomanda.erp.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse createProduct(ProductRequest request, Long organizationId) {
        Product product = new Product();
        product.setOrganizationId(organizationId);
        mapRequestToProduct(request, product, organizationId);
        
        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts(Long organizationId) {
        return productRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id, Long organizationId) {
        Product product = fetchProduct(id, organizationId);
        return mapToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request, Long organizationId) {
        Product product = fetchProduct(id, organizationId);
        mapRequestToProduct(request, product, organizationId);
        
        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(Long id, Long organizationId) {
        Product product = fetchProduct(id, organizationId);
        productRepository.delete(product);
    }

    private Product fetchProduct(Long id, Long organizationId) {
        return productRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Product not found or access denied"));
    }

    private void mapRequestToProduct(ProductRequest request, Product product, Long organizationId) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setStock(request.stock());
        product.setUnitPrice(request.unitPrice());
        product.setDiscount(request.discount());
        product.setExpirationDate(request.expirationDate());

        Set<Category> validCategories = new HashSet<>();
        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            for (Long catId : request.categoryIds()) {
                Category category = categoryRepository.findByIdAndOrganizationId(catId, organizationId)
                        .orElseThrow(() -> new RuntimeException("Category ID " + catId + " not found or access denied"));
                validCategories.add(category);
            }
        }
        product.setCategories(validCategories);
    }

    private ProductResponse mapToResponse(Product product) {
        List<Long> categoryIds = product.getCategories() != null 
                ? product.getCategories().stream().map(Category::getId).collect(Collectors.toList())
                : List.of();

        List<String> categoryNames = product.getCategories() != null 
                ? product.getCategories().stream().map(Category::getName).collect(Collectors.toList())
                : List.of();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStock(),
                product.getUnitPrice(),
                product.getDiscount(),
                product.getExpirationDate(),
                categoryIds,
                categoryNames
        );
    }
}
