package com.example.demo.review.repository;

import com.example.demo.product.entity.Product;
import com.example.demo.review.entity.Review;
import com.example.demo.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductAndIsApprovedTrue(Product product, Pageable pageable);
    List<Review> findByProduct(Product product);
    Optional<Review> findByUserAndProduct(User user, Product product);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product AND r.isApproved = true")
    Double getAverageRating(@Param("product") Product product);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product AND r.isApproved = true")
    Long getReviewCount(@Param("product") Product product);
}

