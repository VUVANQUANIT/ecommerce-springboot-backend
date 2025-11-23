package com.example.demo.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long cartId;
    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String couponCode;
}

