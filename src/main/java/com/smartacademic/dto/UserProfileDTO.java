package com.smartacademic.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

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

    // Dành cho Giảng viên
    private String specialization;
    private String bio;
    private Long departmentId;
}