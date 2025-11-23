package com.example.demo.inventory.entity;

public enum ReservationStatus {
    PENDING,    // Đang chờ thanh toán
    CONFIRMED,  // Đã xác nhận (đã thanh toán)
    RELEASED,   // Đã giải phóng (hết hạn hoặc hủy)
    EXPIRED     // Đã hết hạn
}

