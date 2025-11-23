package com.example.demo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressResponse {
    private String recipientName;
    private String phone;
    private String addressLine;
    private String ward;
    private String district;
    private String city;
    private String postalCode;
}

