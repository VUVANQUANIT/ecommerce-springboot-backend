package com.example.demo.order.entity;

public enum OrderStatus {
    CREATED,      // Đã tạo đơn hàng
    PAID,         // Đã thanh toán
    CONFIRMED,    // Đã xác nhận
    PROCESSING,   // Đang xử lý
    SHIPPED,      // Đã giao hàng
    DELIVERED,    // Đã nhận hàng
    CANCELLED,    // Đã hủy
    RETURNED      // Đã trả hàng
}

