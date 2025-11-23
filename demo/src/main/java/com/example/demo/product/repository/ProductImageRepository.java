package com.example.demo.product.repository;

import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProduct(Product product);
    Optional<ProductImage> findByProductAndIsPrimaryTrue(Product product);
}

