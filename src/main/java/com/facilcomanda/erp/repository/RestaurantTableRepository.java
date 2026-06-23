package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.RestaurantTable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    Optional<RestaurantTable> findByIdAndOrganizationId(Long id, Long organizationId);
    List<RestaurantTable> findByOrganizationId(Long organizationId);
    List<RestaurantTable> findByFloorIdAndOrganizationId(Long floorId, Long organizationId);
}
