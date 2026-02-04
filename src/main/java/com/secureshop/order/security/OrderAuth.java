package com.secureshop.order.security;

import com.secureshop.order.repo.OrderRepository;
import com.secureshop.user.service.UserLookupService;
import org.springframework.stereotype.Component;

@Component("orderAuth")
public class OrderAuth {

    private final OrderRepository orders;
    private final UserLookupService userLookup;

    public OrderAuth(OrderRepository orders, UserLookupService userLookup) {
        this.orders = orders;
        this.userLookup = userLookup;
    }

    public boolean isOwner(Long orderId, String email) {
        Long userId = userLookup.requireUserIdByEmail(email);
        return orders.existsByIdAndUserId(orderId, userId);
    }
}