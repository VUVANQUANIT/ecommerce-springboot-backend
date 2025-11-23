package com.example.demo.cart.controller;

import com.example.demo.cart.dto.*;
import com.example.demo.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "API quản lý giỏ hàng")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Lấy giỏ hàng của người dùng hiện tại")
    public ResponseEntity<CartResponse> getCart() {
        CartResponse response = cartService.getCart();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ hàng")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateCartItem(itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng")
    public ResponseEntity<CartResponse> removeCartItem(@PathVariable Long itemId) {
        CartResponse response = cartService.removeCartItem(itemId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply-coupon")
    @Operation(summary = "Áp dụng mã giảm giá")
    public ResponseEntity<CartResponse> applyCoupon(@Valid @RequestBody ApplyCouponRequest request) {
        CartResponse response = cartService.applyCoupon(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ResponseEntity<CartResponse> clearCart() {
        CartResponse response = cartService.clearCart();
        return ResponseEntity.ok(response);
    }
}

