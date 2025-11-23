package com.example.demo.product.service;

import com.example.demo.catalog.entity.Category;
import com.example.demo.catalog.repository.CategoryRepository;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.product.dto.ProductRequest;
import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.dto.ProductUpdateRequest;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductStatus;
import com.example.demo.product.repository.ProductImageRepository;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // Tìm category - tạm thời lấy category đầu tiên hoặc tạo mới
        Category category = categoryRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Category", 1L));
        
        // Generate SKU
        String sku = "PROD-" + System.currentTimeMillis();
        
        Product product = Product.builder()
                .sku(sku)
                .name(request.getName())
                .description(request.getDescription())
                .listPrice(request.getPrice())
                .price(request.getPrice())
                .category(category)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCategory() != null) {
            // Tìm category mới nếu có - có thể là slug hoặc ID
            Category category = categoryRepository.findBySlug(request.getCategory())
                    .orElse(null);
            if (category == null) {
                try {
                    Long categoryId = Long.parseLong(request.getCategory());
                    category = categoryRepository.findById(categoryId).orElse(null);
                } catch (NumberFormatException e) {
                    // Không phải số, bỏ qua
                }
            }
            if (category != null) {
                product.setCategory(category);
            }
        }

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        product.setStatus(ProductStatus.DELETED);
        productRepository.save(product);
    }

    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        
        if (product.getStatus() == ProductStatus.DELETED) {
            throw new RuntimeException("Sản phẩm đã bị xóa");
        }
        
        return mapToResponse(product);
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponse);
    }

    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(keyword, ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponse);
    }

    public Page<ProductResponse> getProductsByCategory(String categorySlug, Pageable pageable) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category với slug " + categorySlug + " không tồn tại"));
        Page<Product> products = productRepository.findByCategoryAndStatus(category, ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponse);
    }

    public Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Page<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponse);
    }

    public Page<ProductResponse> getMyProducts(Pageable pageable) {
        // Tạm thời trả về tất cả products - sẽ cần thêm seller field hoặc cách khác
        Page<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponse);
    }

    public List<ProductResponse> getMyProductsList() {
        // Tạm thời trả về tất cả products
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse updateProductStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        product.setStatus(status);
        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    private ProductResponse mapToResponse(Product product) {
        // Lấy images từ ProductImage repository
        List<String> images = productImageRepository.findByProduct(product).stream()
                .map(img -> img.getUrl())
                .collect(Collectors.toList());
        
        // Tính tổng stock từ variants (nếu có)
        Integer totalStock = 0; // TODO: Tính từ ProductVariant
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(totalStock)
                .sellerId(null) // TODO: Thêm seller nếu cần
                .sellerName(null)
                .sellerEmail(null)
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .images(images)
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
