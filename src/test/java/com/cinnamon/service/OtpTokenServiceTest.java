package com.cinnamon.service;

import com.cinnamon.enums.OtpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test cho OtpTokenService bằng JUnit 5.
 * Kiểm thử toàn bộ các kịch bản của cơ chế Stateless OTP Token (HMAC-SHA256).
 */
class OtpTokenServiceTest {

    private OtpTokenService otpTokenService;

    @BeforeEach
    void setUp() throws Exception {
        // Khởi tạo OtpTokenService với secret key chuẩn 256-bit
        String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        otpTokenService = new OtpTokenService(testSecret);
    }

    @Test
    @DisplayName("OTP-01: Nhập đúng mã OTP và đúng email thì xác thực thành công (OK)")
    void verify_DungMaVaDungEmail_TraVeOK() {
        // 1. Arrange: Sinh token và mã OTP 6 số cho email test
        OtpTokenService.Issued issued = otpTokenService.issue("student@cinnamon.com");

        // 2. Act: Xác thực với đúng email và đúng mã vừa sinh
        OtpResult result = otpTokenService.verify(
                issued.token(),
                "student@cinnamon.com",
                issued.otp()
        );

        // 3. Assert: Kết quả phải là OK
        assertThat(result).isEqualTo(OtpResult.OK);
    }

    @Test
    @DisplayName("OTP-02: Nhập sai mã OTP 6 số thì trả về WRONG")
    void verify_SaiMaOtp_TraVeWRONG() {
        // 1. Arrange: Sinh token cho email
        OtpTokenService.Issued issued = otpTokenService.issue("student@cinnamon.com");

        // 2. Act: Cố tình nhập mã OTP sai ("000000")
        OtpResult result = otpTokenService.verify(
                issued.token(),
                "student@cinnamon.com",
                "000000"
        );

        // 3. Assert: Kết quả phải trả về WRONG
        assertThat(result).isEqualTo(OtpResult.WRONG);
    }

    @Test
    @DisplayName("OTP-03: Dùng token của email này để xác thực cho email khác thì trả về INVALID")
    void verify_TokenKhongKhopEmail_TraVeINVALID() {
        // 1. Arrange: Sinh token cho email A
        OtpTokenService.Issued issued = otpTokenService.issue("userA@cinnamon.com");

        // 2. Act: Kẻ gian dùng token của userA nhưng lại gửi kèm email của userB
        OtpResult result = otpTokenService.verify(
                issued.token(),
                "userB@cinnamon.com",
                issued.otp()
        );

        // 3. Assert: Hệ thống phát hiện email không khớp với subject trong token -> INVALID
        assertThat(result).isEqualTo(OtpResult.INVALID);
    }

    @Test
    @DisplayName("OTP-04: Truyền token rác hoặc token giả mạo thì trả về INVALID")
    void verify_TokenGiaMao_TraVeINVALID() {
        // Act: Truyền một chuỗi chuỗi rác không phải JWT hợp lệ
        OtpResult result = otpTokenService.verify(
                "token_rac_khong_hop_le_123456",
                "student@cinnamon.com",
                "123456"
        );

        // Assert: Thư viện JWT không parse được -> INVALID
        assertThat(result).isEqualTo(OtpResult.INVALID);
    }

    @Test
    @DisplayName("OTP-05: Truyền token rỗng hoặc null thì trả về INVALID")
    void verify_TokenRong_TraVeINVALID() {
        assertThat(otpTokenService.verify("", "student@cinnamon.com", "123456"))
                .isEqualTo(OtpResult.INVALID);

        assertThat(otpTokenService.verify(null, "student@cinnamon.com", "123456"))
                .isEqualTo(OtpResult.INVALID);
    }
}

