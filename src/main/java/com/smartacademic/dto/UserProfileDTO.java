package com.smartacademic.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Data
public class UserProfileDTO {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100)
    private String fullName;

    @Pattern(regexp = "^(0|\\+84)[0-9]{8,9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String studentCode;
    private String className;
    private String avatarUrl;

    private String specialization;
    private String bio;
    private Long departmentId;

    //Giảng viên: phí cho 1 buổi tư vấn (VND). 0 = miễn phí.
    @DecimalMin(value = "0", message = "Phí buổi tư vấn không được âm")
    private BigDecimal sessionFee;
}