package com.example.demo.order.dto;

import com.example.demo.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private ShippingAddressResponse shippingAddress;
    private String paymentInfo;
    private String note;
    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

