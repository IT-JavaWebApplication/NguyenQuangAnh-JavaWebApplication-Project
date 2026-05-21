package com.smartacademic.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class LoginDTO {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}