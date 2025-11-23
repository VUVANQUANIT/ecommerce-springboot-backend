package com.example.demo.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String productImage;
    private Map<String, String> variantAttributes; // size, color, etc.
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private Integer availableStock;
}

