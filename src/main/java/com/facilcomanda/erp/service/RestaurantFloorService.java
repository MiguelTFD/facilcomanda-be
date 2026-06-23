package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.RestaurantFloorRequest;
import com.facilcomanda.erp.dto.RestaurantFloorResponse;
import com.facilcomanda.erp.model.RestaurantFloor;
import com.facilcomanda.erp.repository.RestaurantFloorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantFloorService {

    private final RestaurantFloorRepository restaurantFloorRepository;

    public RestaurantFloorService(RestaurantFloorRepository restaurantFloorRepository) {
        this.restaurantFloorRepository = restaurantFloorRepository;
    }

    public RestaurantFloorResponse createFloor(RestaurantFloorRequest request, Long organizationId) {
        RestaurantFloor floor = new RestaurantFloor();
        floor.setOrganizationId(organizationId);
        floor.setName(request.name());
        floor.setDescription(request.description());

        RestaurantFloor savedFloor = restaurantFloorRepository.save(floor);
        return mapToResponse(savedFloor);
    }

    public List<RestaurantFloorResponse> getAllFloors(Long organizationId) {
        return restaurantFloorRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RestaurantFloorResponse getFloorById(Long id, Long organizationId) {
        RestaurantFloor floor = fetchFloor(id, organizationId);
        return mapToResponse(floor);
    }

    public RestaurantFloorResponse updateFloor(Long id, RestaurantFloorRequest request, Long organizationId) {
        RestaurantFloor floor = fetchFloor(id, organizationId);
        floor.setName(request.name());
        floor.setDescription(request.description());

        RestaurantFloor updatedFloor = restaurantFloorRepository.save(floor);
        return mapToResponse(updatedFloor);
    }

    public void deleteFloor(Long id, Long organizationId) {
        RestaurantFloor floor = fetchFloor(id, organizationId);
        restaurantFloorRepository.delete(floor);
    }

    public RestaurantFloor fetchFloor(Long id, Long organizationId) {
        return restaurantFloorRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Floor not found or unauthorized"));
    }

    private RestaurantFloorResponse mapToResponse(RestaurantFloor floor) {
        return new RestaurantFloorResponse(
                floor.getId(),
                floor.getName(),
                floor.getDescription(),
                floor.getOrganizationId()
        );
    }
}
