package com.example.demo.product.repository;

import com.example.demo.catalog.entity.Brand;
import com.example.demo.catalog.entity.Category;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findBySku(String sku);
    Optional<Product> findBySlug(String slug);
    
    // Tìm sản phẩm đang active
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    // Tìm kiếm sản phẩm theo tên hoặc mô tả
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND p.status = :status")
    Page<Product> searchProducts(@Param("keyword") String keyword, 
                                  @Param("status") ProductStatus status, 
                                  Pageable pageable);
    
    // Tìm sản phẩm theo category
    Page<Product> findByCategoryAndStatus(Category category, ProductStatus status, Pageable pageable);
    
    // Tìm sản phẩm theo brand
    Page<Product> findByBrandAndStatus(Brand brand, ProductStatus status, Pageable pageable);
    
    // Tìm sản phẩm theo khoảng giá
    @Query("SELECT p FROM Product p WHERE " +
           "p.price BETWEEN :minPrice AND :maxPrice " +
           "AND p.status = :status")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("status") ProductStatus status,
                                    Pageable pageable);
    
    // Tìm kiếm nâng cao với nhiều filters
    @Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status")
    Page<Product> searchWithFilters(@Param("category") Category category,
                                     @Param("brand") Brand brand,
                                     @Param("minPrice") BigDecimal minPrice,
                                     @Param("maxPrice") BigDecimal maxPrice,
                                     @Param("keyword") String keyword,
                                     @Param("status") ProductStatus status,
                                     Pageable pageable);
}

