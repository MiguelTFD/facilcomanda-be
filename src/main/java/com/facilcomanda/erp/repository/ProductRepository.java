package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.categories WHERE p.organizationId = :organizationId")
    List<Product> findByOrganizationId(@Param("organizationId") Long organizationId);
    
    Optional<Product> findByIdAndOrganizationId(Long id, Long organizationId);
}
