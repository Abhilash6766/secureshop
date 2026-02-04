package com.secureshop.order.dto;

import jakarta.validation.constraints.Min;

public record CreateOrderRequest(
        @Min(0) long totalCents
) {}