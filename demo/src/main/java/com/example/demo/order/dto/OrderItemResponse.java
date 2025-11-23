package com.example.demo.order.dto;

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
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long variantId;
    private String variantSku;
    private Map<String, String> variantAttributes;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
}

