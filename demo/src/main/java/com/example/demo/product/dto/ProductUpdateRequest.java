package com.example.demo.product.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    
    @Size(min = 1, max = 200, message = "Tên sản phẩm phải từ 1 đến 200 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0")
    @Digits(integer = 15, fraction = 2, message = "Giá sản phẩm không hợp lệ")
    private BigDecimal price;

    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stock;

    private String category;

    private List<String> images;
}

