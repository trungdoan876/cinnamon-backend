package com.cinnamon.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    /**
     * Mục đích gửi OTP: REGISTER (đăng ký tài khoản) hoặc LOGIN (đăng nhập).
     * Mặc định là LOGIN nếu không truyền.
     */
    private String purpose;
}
