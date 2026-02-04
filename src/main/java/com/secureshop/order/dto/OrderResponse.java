package com.secureshop.order.dto;

public record OrderResponse(
        Long id,
        Long totalCents,
        String status
) {}