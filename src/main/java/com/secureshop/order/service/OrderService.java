package com.secureshop.order.service;

import com.secureshop.order.domain.Order;
import com.secureshop.order.dto.OrderResponse;
import com.secureshop.order.repo.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public OrderResponse create(Long userId, long totalCents) {
        Order order = Order.builder()
                .userId(userId)
                .totalCents(totalCents)
                .status("CREATED")
                .build();
        Order saved = orders.save(order);
        return new OrderResponse(saved.getId(), saved.getTotalCents(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return new OrderResponse(order.getId(), order.getTotalCents(), order.getStatus());
    }

    @Transactional
    public OrderResponse refund(Long orderId) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // atomic state transition under lock
        if ("REFUNDED".equalsIgnoreCase(order.getStatus())) {
            // idempotent behavior: no double-side effects
            return new OrderResponse(order.getId(), order.getTotalCents(), order.getStatus());
        }

        order.setStatus("REFUNDED");
        Order saved = orders.save(order);
        return new OrderResponse(saved.getId(), saved.getTotalCents(), saved.getStatus());
    }
}
