package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.RestaurantFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantFloorRepository extends JpaRepository<RestaurantFloor, Long> {
    Optional<RestaurantFloor> findByIdAndOrganizationId(Long id, Long organizationId);
    List<RestaurantFloor> findByOrganizationId(Long organizationId);
}
