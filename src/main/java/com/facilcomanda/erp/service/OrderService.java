package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.OrderItemRequest;
import com.facilcomanda.erp.dto.OrderItemResponse;
import com.facilcomanda.erp.dto.OrderRequest;
import com.facilcomanda.erp.dto.OrderResponse;
import com.facilcomanda.erp.dto.OrderStatusRequest;
import com.facilcomanda.erp.event.OrderCreatedEvent;
import com.facilcomanda.erp.exception.OrderNotAttendableException;
import com.facilcomanda.erp.exception.OrderReductionNotAllowedException;
import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import com.facilcomanda.erp.model.Product;
import com.facilcomanda.erp.model.RestaurantTable;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.model.enums.TableState;
import com.facilcomanda.erp.repository.OrderRepository;
import com.facilcomanda.erp.repository.ProductRepository;
import com.facilcomanda.erp.repository.RestaurantTableRepository;
import com.facilcomanda.erp.repository.UserRepository;
import com.facilcomanda.erp.model.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, RestaurantTableRepository restaurantTableRepository,
            ProductRepository productRepository, UserRepository userRepository, Clock clock,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request, Long organizationId, String userEmail) {
        if (orderRepository.existsByIdempotencyKeyAndOrganizationId(request.idempotencyKey(), organizationId)) {
            throw new RuntimeException("Order already processed");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RestaurantTable table = null;
        if (request.restaurantTableId() != null) {
            table = restaurantTableRepository.findByIdAndOrganizationId(request.restaurantTableId(), organizationId)
                    .orElseThrow(() -> new RuntimeException("Restaurant table not found or unauthorized"));
            
            // Auto-occupy the table
            table.setState(TableState.OCCUPIED);
            restaurantTableRepository.save(table);
        }

        Order order = new Order();
        order.setOrganizationId(organizationId);
        order.setRestaurantTable(table);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setType(request.type());
        order.setOrderDate(LocalDateTime.now(clock));
        order.setIdempotencyKey(request.idempotencyKey());

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findByIdAndOrganizationId(itemRequest.productId(), organizationId)
                    .orElseThrow(() -> new RuntimeException(
                            "Product ID " + itemRequest.productId() + " not found or unauthorized"));

            if (product.getStock() < itemRequest.quantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            product.setStock(product.getStock() - itemRequest.quantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrganizationId(organizationId);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setComments(itemRequest.comments());
            // Ronda 1: los ítems creados con la orden (feature 025, D1).
            orderItem.setRoundNumber(1);
            orderItem.setCreatedAt(order.getOrderDate());

            BigDecimal price = product.getUnitPrice();
            if (product.getDiscount() != null) {
                price = price.subtract(product.getDiscount());
            }

            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemRequest.quantity()));
            orderItem.setSubtotal(subtotal);

            order.addOrderItem(orderItem);
            total = total.add(subtotal);
        }

        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);
        
        // Publicación del evento para desacoplar infraestructura (Impresora)
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));
        
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrders(Long organizationId) {
        return orderRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingPaymentOrdersWithOccupiedTables(Long organizationId) {
        return orderRepository.findByOrganizationIdAndStatusInAndRestaurantTable_State(
                        organizationId,
                        List.of(OrderStatus.PENDING, OrderStatus.DELIVERED),
                        TableState.OCCUPIED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrderStatus(Long id, OrderStatusRequest request, Long organizationId) {
        Order order = orderRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Order not found or unauthorized"));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Paid orders cannot be modified");
        }

        if (request.status() == OrderStatus.PAID) {
            throw new RuntimeException("Use the charge endpoint to mark an order as paid");
        }

        order.setStatus(request.status());

        RestaurantTable table = order.getRestaurantTable();
        if (table != null && request.status() == OrderStatus.CANCELLED) {
            table.setState(TableState.AVAILABLE);
            restaurantTableRepository.save(table);
        }

        return mapToResponse(orderRepository.save(order));
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse markAttended(Long id, Long organizationId) {
        Order order = orderRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Order not found or unauthorized"));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderNotAttendableException(
                    "Paid or cancelled orders cannot be marked as attended");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            return mapToResponse(order);
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setAttendedAt(LocalDateTime.now(clock));

        return mapToResponse(orderRepository.save(order));
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrder(Long id, OrderRequest request, Long organizationId) {
        Order order = orderRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Order not found or unauthorized"));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Paid or cancelled orders cannot be modified");
        }

        Map<Long, Integer> persistedByProduct = new LinkedHashMap<>();
        int currentMaxRound = 0;
        for (OrderItem existing : order.getOrderItems()) {
            persistedByProduct.merge(existing.getProduct().getId(), existing.getQuantity(), Integer::sum);
            if (existing.getRoundNumber() != null) {
                currentMaxRound = Math.max(currentMaxRound, existing.getRoundNumber());
            }
        }

        Map<Long, Integer> incomingByProduct = new LinkedHashMap<>();
        Map<Long, String> commentsByProduct = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : request.items()) {
            incomingByProduct.merge(itemRequest.productId(), itemRequest.quantity(), Integer::sum);
            commentsByProduct.put(itemRequest.productId(), itemRequest.comments());
        }

        for (Map.Entry<Long, Integer> persisted : persistedByProduct.entrySet()) {
            int incoming = incomingByProduct.getOrDefault(persisted.getKey(), 0);
            if (incoming < persisted.getValue()) {
                throw new OrderReductionNotAllowedException(
                        "Cannot reduce quantity already sent to the kitchen for product ID " + persisted.getKey());
            }
        }

        Map<Long, Product> productsByDelta = new LinkedHashMap<>();
        Map<Long, Integer> deltaByProduct = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> incoming : incomingByProduct.entrySet()) {
            int delta = incoming.getValue() - persistedByProduct.getOrDefault(incoming.getKey(), 0);
            if (delta <= 0) {
                continue;
            }
            Product product = productRepository.findByIdAndOrganizationId(incoming.getKey(), organizationId)
                    .orElseThrow(() -> new RuntimeException(
                            "Product ID " + incoming.getKey() + " not found or unauthorized"));
            if (product.getStock() < delta) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            productsByDelta.put(incoming.getKey(), product);
            deltaByProduct.put(incoming.getKey(), delta);
        }

        int newRound = currentMaxRound + 1;
        LocalDateTime now = LocalDateTime.now(clock);
        for (Map.Entry<Long, Integer> entry : deltaByProduct.entrySet()) {
            Product product = productsByDelta.get(entry.getKey());
            int delta = entry.getValue();

            product.setStock(product.getStock() - delta);
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrganizationId(organizationId);
            orderItem.setProduct(product);
            orderItem.setQuantity(delta);
            orderItem.setRoundNumber(newRound);
            orderItem.setCreatedAt(now);

            BigDecimal price = product.getUnitPrice();
            if (product.getDiscount() != null) {
                price = price.subtract(product.getDiscount());
            }
            orderItem.setSubtotal(price.multiply(BigDecimal.valueOf(delta)));

            order.addOrderItem(orderItem);
        }

        Map<Long, OrderItem> commentTargetByProduct = new LinkedHashMap<>();
        for (OrderItem item : order.getOrderItems()) {
            OrderItem current = commentTargetByProduct.get(item.getProduct().getId());
            if (current == null || roundOf(item) >= roundOf(current)) {
                commentTargetByProduct.put(item.getProduct().getId(), item);
            }
        }
        for (Map.Entry<Long, String> comment : commentsByProduct.entrySet()) {
            OrderItem target = commentTargetByProduct.get(comment.getKey());
            if (target != null) {
                target.setComments(normalizeComments(comment.getValue()));
            }
        }

        if (!deltaByProduct.isEmpty() && order.getStatus() == OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.PENDING);
        }

        BigDecimal total = order.getOrderItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);

        return mapToResponse(orderRepository.save(order));
    }

    private int roundOf(OrderItem item) {
        return item.getRoundNumber() != null ? item.getRoundNumber() : 1;
    }

    private String normalizeComments(String comments) {
        return (comments == null || comments.isBlank()) ? null : comments;
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream().map(item -> new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getComments(),
                item.getCreatedAt(),
                item.getRoundNumber())).collect(Collectors.toList());

        boolean modified = order.getOrderItems().stream()
                .anyMatch(item -> item.getRoundNumber() != null && item.getRoundNumber() > 1);

        return new OrderResponse(
                order.getId(),
                order.getRestaurantTable() != null ? order.getRestaurantTable().getId() : null,
                order.getRestaurantTable() != null ? order.getRestaurantTable().getName() : null,
                (order.getRestaurantTable() != null && order.getRestaurantTable().getFloor() != null) ? order.getRestaurantTable().getFloor().getName() : null,
                order.getType(),
                order.getStatus(),
                order.getTotal(),
                order.getOrderDate(),
                itemResponses,
                modified,
                order.getStatus() == OrderStatus.DELIVERED,
                order.getAttendedAt());
    }
}