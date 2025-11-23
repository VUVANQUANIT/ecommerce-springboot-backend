package com.example.demo.cart.repository;

import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndVariantId(Cart cart, Long variantId);
    void deleteByCart(Cart cart);
}

