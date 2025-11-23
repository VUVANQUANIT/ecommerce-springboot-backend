package com.example.demo.product.repository;

import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProduct(Product product);
    
    // Optimistic locking cho stock update
    @Lock(LockModeType.OPTIMISTIC)
    Optional<ProductVariant> findById(Long id);
    
    // Giảm stock với điều kiện
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stock = v.stock - :quantity WHERE v.id = :id AND v.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    // Tăng stock
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stock = v.stock + :quantity WHERE v.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}

