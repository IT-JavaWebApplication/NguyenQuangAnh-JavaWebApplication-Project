package com.smartacademic.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class EquipmentDTO {

    private Long id;

    @NotBlank(message = "Mã thiết bị không được để trống")
    @Size(max = 50, message = "Mã thiết bị không được quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên thiết bị không được để trống")
    @Size(max = 200, message = "Tên thiết bị không được quá 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer available;

    private String unit = "Cái";

    /** Tiền đặt cọc khi mượn thiết bị (VND). 0 = không yêu cầu đặt cọc. */
    @Min(value = 0, message = "Phí đặt cọc không được âm")
    private java.math.BigDecimal depositAmount = java.math.BigDecimal.ZERO;

    private Boolean isActive = true;
}