package com.example.demo.order.service;

import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartRepository;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.inventory.entity.ReservationStatus;
import com.example.demo.inventory.entity.StockReservation;
import com.example.demo.inventory.repository.StockReservationRepository;
import com.example.demo.order.dto.*;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderItem;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.repository.ProductVariantRepository;
import com.example.demo.promotion.entity.Coupon;
import com.example.demo.promotion.repository.CouponRepository;
import com.example.demo.user.entity.Address;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AddressRepository;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponRepository couponRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getShippingAddressId()));

        if (!shippingAddress.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Địa chỉ không thuộc về người dùng này");
        }

        // Tính toán giá
        BigDecimal subtotal = calculateCartSubtotal(cart);
        BigDecimal shippingFee = calculateShippingFee(shippingAddress); // Có thể tính dựa trên khoảng cách, trọng lượng
        BigDecimal discountAmount = BigDecimal.ZERO;

        // Áp dụng coupon nếu có
        Coupon coupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
            coupon = couponRepository.findValidCoupon(request.getCouponCode(), LocalDateTime.now())
                    .orElseThrow(() -> new RuntimeException("Mã coupon không hợp lệ"));

            if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
                throw new RuntimeException("Đơn hàng tối thiểu phải là " + coupon.getMinOrderAmount());
            }

            if (coupon.getType().name().equals("FIXED_AMOUNT")) {
                discountAmount = coupon.getAmount();
            } else { // PERCENTAGE
                discountAmount = subtotal.multiply(coupon.getAmount())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            // Giảm số lần sử dụng coupon
            couponRepository.decrementUsesLeft(coupon.getId());
        }

        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);

        // Tạo đơn hàng
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .shippingAddress(shippingAddress)
                .paymentInfo("{\"method\":\"" + request.getPaymentMethod() + "\"}")
                .note(request.getNote())
                .build();

        // Tạo order items và reserve stock
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();

            // Kiểm tra stock
            if (variant.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + variant.getSku() + " không đủ số lượng");
            }

            // Tạo order item
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(variant.getProduct())
                    .variant(variant)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            order.getItems().add(orderItem);

            // Reserve stock (sẽ được confirm khi thanh toán thành công)
            StockReservation reservation = StockReservation.builder()
                    .variant(variant)
                    .order(order)
                    .quantity(cartItem.getQuantity())
                    .status(ReservationStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(30)) // 30 phút để thanh toán
                    .build();
            stockReservationRepository.save(reservation);

            // Giảm stock tạm thời (sẽ rollback nếu thanh toán thất bại)
            int updated = variantRepository.decreaseStock(variant.getId(), cartItem.getQuantity());
            if (updated == 0) {
                throw new RuntimeException("Không thể giảm stock cho sản phẩm " + variant.getSku());
            }
        }

        Order savedOrder = orderRepository.save(order);

        // Xóa giỏ hàng sau khi checkout thành công
        // cartRepository.delete(cart); // Hoặc giữ lại để user có thể xem lại

        return mapToOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse confirmPayment(String orderNumber, String paymentReference) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new RuntimeException("Đơn hàng đã được xử lý");
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.PAID);
        order.setPaymentInfo(order.getPaymentInfo() + ",\"reference\":\"" + paymentReference + "\"}");

        // Confirm stock reservations
        List<StockReservation> reservations = stockReservationRepository.findByOrder(order);
        for (StockReservation reservation : reservations) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            stockReservationRepository.save(reservation);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(user.getId()) && !isAdmin()) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Không thể hủy đơn hàng đã giao");
        }

        // Release stock reservations
        List<StockReservation> reservations = stockReservationRepository.findByOrder(order);
        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.PENDING || 
                reservation.getStatus() == ReservationStatus.CONFIRMED) {
                // Trả lại stock
                variantRepository.increaseStock(reservation.getVariant().getId(), reservation.getQuantity());
                reservation.setStatus(ReservationStatus.RELEASED);
                stockReservationRepository.save(reservation);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(user.getId()) && !isAdmin()) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User user = getCurrentUser();
        Page<Order> orders = orderRepository.findByUser(user, pageable);
        return orders.map(this::mapToOrderResponse);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        if (!isAdmin()) {
            throw new RuntimeException("Chỉ admin mới có quyền xem tất cả đơn hàng");
        }
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(this::mapToOrderResponse);
    }

    private BigDecimal calculateCartSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateShippingFee(Address address) {
        // TODO: Tính phí vận chuyển dựa trên địa chỉ, trọng lượng, khoảng cách
        // Tạm thời trả về 30000 VNĐ
        return BigDecimal.valueOf(30000);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        ShippingAddressResponse addressResponse = null;
        if (order.getShippingAddress() != null) {
            Address addr = order.getShippingAddress();
            addressResponse = ShippingAddressResponse.builder()
                    .recipientName(addr.getRecipientName())
                    .phone(addr.getPhone())
                    .addressLine(addr.getAddressLine())
                    .ward(addr.getWard())
                    .district(addr.getDistrict())
                    .city(addr.getCity())
                    .postalCode(addr.getPostalCode())
                    .build();
        }

        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFullName())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(addressResponse)
                .paymentInfo(order.getPaymentInfo())
                .note(order.getNote())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .variantSku(item.getVariant() != null ? item.getVariant().getSku() : null)
                .variantAttributes(item.getVariant() != null ? item.getVariant().getAttributes() : null)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

