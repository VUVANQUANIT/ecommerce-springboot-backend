package com.example.demo.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private Long shippingAddressId;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // stripe, paypal, vnpay, etc.

    private String couponCode;

    private String note;
}

