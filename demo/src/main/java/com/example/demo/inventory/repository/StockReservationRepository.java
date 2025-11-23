package com.example.demo.inventory.repository;

import com.example.demo.inventory.entity.ReservationStatus;
import com.example.demo.inventory.entity.StockReservation;
import com.example.demo.order.entity.Order;
import com.example.demo.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByOrder(Order order);
    List<StockReservation> findByVariantAndStatus(ProductVariant variant, ReservationStatus status);
    
    @Query("SELECT sr FROM StockReservation sr WHERE sr.expiresAt < :now AND sr.status = 'PENDING'")
    List<StockReservation> findExpiredReservations(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE StockReservation sr SET sr.status = :status WHERE sr.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") ReservationStatus status);
}

