package com.secureshop.order.controller;

import com.secureshop.audit.AuditedAction;
import com.secureshop.idempotency.service.IdempotencyService;
import com.secureshop.idempotency.util.RequestHasher;
import com.secureshop.order.dto.CreateOrderRequest;
import com.secureshop.order.dto.OrderResponse;
import com.secureshop.order.service.OrderService;
import com.secureshop.user.service.UserLookupService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserLookupService userLookup;
    private final IdempotencyService idempotency;
    private final RequestHasher requestHasher;

    public OrderController(OrderService orderService, UserLookupService userLookup, IdempotencyService idempotency, RequestHasher requestHasher) {
        this.orderService = orderService;
        this.userLookup = userLookup;
        this.idempotency = idempotency;
        this.requestHasher = requestHasher;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req, Authentication auth) {
        Long userId = userLookup.requireUserIdByEmail(auth.getName());
        return orderService.create(userId, req.totalCents());
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("@orderAuth.isOwner(#orderId, authentication.name) or hasAuthority('ORDER_READ_ALL')")
    public OrderResponse get(@PathVariable Long orderId) {
        return orderService.get(orderId);
    }

    @AuditedAction(action = "ORDER_REFUND", entityType = "order", entityIdParam = "orderId")
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("@orderAuth.isOwner(#orderId, authentication.name) or hasAuthority('ORDER_REFUND')")
    public Object refund(@PathVariable Long orderId,
                         Authentication auth,
                         @RequestHeader("Idempotency-Key") String idemKey) {

        Long userId = userLookup.requireUserIdByEmail(auth.getName());
        String endpoint = "POST:/api/v1/orders/{orderId}/refund";
        String requestHash = requestHasher.sha256("refund:" + userId + ":" + orderId);

        return idempotency.execute(
                userId, endpoint, idemKey, requestHash,
                () -> orderService.refund(orderId)
        ).body(); // ✅ return body only
    }

}