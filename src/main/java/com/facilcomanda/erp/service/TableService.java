package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.TableRequest;
import com.facilcomanda.erp.dto.TableResponse;
import com.facilcomanda.erp.model.RestaurantFloor;
import com.facilcomanda.erp.model.RestaurantTable;
import com.facilcomanda.erp.repository.RestaurantFloorRepository;
import com.facilcomanda.erp.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TableService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final RestaurantFloorRepository restaurantFloorRepository;

    public TableService(RestaurantTableRepository restaurantTableRepository, RestaurantFloorRepository restaurantFloorRepository) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.restaurantFloorRepository = restaurantFloorRepository;
    }

    public TableResponse createTable(TableRequest request, Long organizationId) {
        RestaurantFloor floor = restaurantFloorRepository.findByIdAndOrganizationId(request.floorId(), organizationId)
                .orElseThrow(() -> new RuntimeException("Floor not found or unauthorized"));

        RestaurantTable table = new RestaurantTable();
        table.setOrganizationId(organizationId);
        table.setFloor(floor);
        mapRequestToTable(request, table);

        RestaurantTable savedTable = restaurantTableRepository.save(table);
        return mapToResponse(savedTable);
    }

    public List<TableResponse> getAllTables(Long organizationId) {
        return restaurantTableRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TableResponse> getTablesByFloorAndOrganization(Long floorId, Long organizationId) {
        return restaurantTableRepository.findByFloorIdAndOrganizationId(floorId, organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TableResponse getTableById(Long id, Long organizationId) {
        RestaurantTable table = fetchTable(id, organizationId);
        return mapToResponse(table);
    }

    public TableResponse updateTable(Long id, TableRequest request, Long organizationId) {
        RestaurantTable table = fetchTable(id, organizationId);
        
        RestaurantFloor floor = restaurantFloorRepository.findByIdAndOrganizationId(request.floorId(), organizationId)
                .orElseThrow(() -> new RuntimeException("Floor not found or unauthorized"));
        table.setFloor(floor);
        
        mapRequestToTable(request, table);

        RestaurantTable updatedTable = restaurantTableRepository.save(table);
        return mapToResponse(updatedTable);
    }

    public void deleteTable(Long id, Long organizationId) {
        RestaurantTable table = fetchTable(id, organizationId);
        restaurantTableRepository.delete(table);
    }

    private RestaurantTable fetchTable(Long id, Long organizationId) {
        return restaurantTableRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Table not found or unauthorized"));
    }

    private void mapRequestToTable(TableRequest request, RestaurantTable table) {
        table.setName(request.name());
        table.setDescription(request.description());
        table.setState(request.state());
        table.setChairs(request.chairs());
    }

    private TableResponse mapToResponse(RestaurantTable table) {
        return new TableResponse(
                table.getId(),
                table.getName(),
                table.getDescription(),
                table.getState(),
                table.getChairs(),
                table.getOrganizationId(),
                table.getFloor() != null ? table.getFloor().getId() : null,
                table.getFloor() != null ? table.getFloor().getName() : null
        );
    }
}
