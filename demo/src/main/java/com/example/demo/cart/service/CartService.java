package com.example.demo.cart.service;

import com.example.demo.cart.dto.*;
import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.cart.repository.CartRepository;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.repository.ProductImageRepository;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.product.repository.ProductVariantRepository;
import com.example.demo.promotion.entity.Coupon;
import com.example.demo.promotion.repository.CouponRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> createCart(user));

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> createCart(user));

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", request.getVariantId()));

        // Kiểm tra stock
        if (variant.getStock() < request.getQuantity()) {
            throw new RuntimeException("Số lượng sản phẩm không đủ. Còn lại: " + variant.getStock());
        }

        // Tìm item đã có trong giỏ
        CartItem existingItem = cartItemRepository.findByCartAndVariantId(cart, request.getVariantId())
                .orElse(null);

        if (existingItem != null) {
            // Cập nhật số lượng
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (newQuantity > variant.getStock()) {
                throw new RuntimeException("Số lượng vượt quá tồn kho. Còn lại: " + variant.getStock());
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setPrice(variant.getPrice()); // Cập nhật giá mới nhất
            cartItemRepository.save(existingItem);
        } else {
            // Tạo item mới
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .price(variant.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(Long itemId, UpdateCartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "user", user.getId()));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa item này");
        }

        // Kiểm tra stock
        if (request.getQuantity() > item.getVariant().getStock()) {
            throw new RuntimeException("Số lượng vượt quá tồn kho. Còn lại: " + item.getVariant().getStock());
        }

        item.setQuantity(request.getQuantity());
        item.setPrice(item.getVariant().getPrice()); // Cập nhật giá mới nhất
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeCartItem(Long itemId) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "user", user.getId()));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa item này");
        }

        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse applyCoupon(ApplyCouponRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "user", user.getId()));

        Coupon coupon = couponRepository.findValidCoupon(request.getCouponCode(), LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Mã coupon không hợp lệ hoặc đã hết hạn"));

        // Tính tổng tiền giỏ hàng
        BigDecimal subtotal = calculateSubtotal(cart);
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Đơn hàng tối thiểu phải là " + coupon.getMinOrderAmount());
        }

        // Lưu coupon vào cart (có thể thêm field couponCode vào Cart entity)
        // Tạm thời tính discount và trả về
        return buildCartResponse(cart, coupon);
    }

    @Transactional
    public CartResponse clearCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "user", user.getId()));

        cartItemRepository.deleteByCart(cart);
        return buildCartResponse(cart);
    }

    private Cart createCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();
        return cartRepository.save(cart);
    }

    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartResponse buildCartResponse(Cart cart) {
        return buildCartResponse(cart, null);
    }

    private CartResponse buildCartResponse(Cart cart, Coupon coupon) {
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;

        if (coupon != null && subtotal.compareTo(coupon.getMinOrderAmount()) >= 0) {
            couponCode = coupon.getCode();
            if (coupon.getType().name().equals("FIXED_AMOUNT")) {
                discountAmount = coupon.getAmount();
            } else { // PERCENTAGE
                discountAmount = subtotal.multiply(coupon.getAmount())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal total = subtotal.subtract(discountAmount);

        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .total(total)
                .couponCode(couponCode)
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();

        // Lấy ảnh chính của sản phẩm
        String productImage = productImageRepository.findByProductAndIsPrimaryTrue(product)
                .map(img -> img.getUrl())
                .orElse("");

        return CartItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(productImage)
                .variantAttributes(variant.getAttributes())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableStock(variant.getStock())
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }
}

