package com.example.demo.promotion.repository;

import com.example.demo.promotion.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.isActive = true " +
           "AND c.validFrom <= :now AND c.validTo >= :now AND c.usesLeft > 0")
    Optional<Coupon> findValidCoupon(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Coupon c SET c.usesLeft = c.usesLeft - 1 WHERE c.id = :id AND c.usesLeft > 0")
    int decrementUsesLeft(@Param("id") Long id);
}

