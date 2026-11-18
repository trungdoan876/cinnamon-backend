package com.cinnamon.service;

import com.cinnamon.dto.request.LoginRequest;
import com.cinnamon.dto.request.RegisterRequest;
import com.cinnamon.dto.response.AuthResponse;
import com.cinnamon.entity.User;
import com.cinnamon.enums.OtpResult;
import com.cinnamon.enums.Role;
import com.cinnamon.exception.BadRequestException;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.UserRepository;
import com.cinnamon.repository.UserXpRepository;
import com.cinnamon.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho tính năng Đăng ký tài khoản (AuthService.register)
 * Kết hợp JUnit 5 + Mockito theo cấu trúc Arrange - Act - Assert.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserXpRepository userXpRepository;

    @Mock
    private OtpTokenService otpTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@cinnamon.com");
        validRequest.setPassword("Password123@");
        validRequest.setFullName("Test User");
        validRequest.setOtp("123456");
        validRequest.setOtpToken("mock_jwt_otp_token");
    }

    @Test
    @DisplayName("REG-01: Đăng ký thành công khi thông tin hợp lệ và OTP đúng")
    void register_ThanhCong_TraVeAuthResponseVaToken() {
        // ARRANGE - tạo dữ liệu giả
        // res chưa có khi hỏi đến user này
        when(userRepository.existsByEmail("test@cinnamon.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(otpTokenService.verify("mock_jwt_otp_token", "test@cinnamon.com", "123456"))
                .thenReturn(OtpResult.OK);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        // Giả lập lưu user vào DB
        // trả về user kèm ID
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L); // tự gán id = 1
            return u;
        });

        when(jwtService.generateToken(any(User.class))).thenReturn("mock_access_jwt_token");

        // ACT - Thực thi đăng ký
        AuthResponse response = authService.register(validRequest);

        // ASSERT - Kiểm chứng kết quả
        // ko được rỗng
        assertThat(response).isNotNull();
        // phải có token
        assertThat(response.getToken()).isEqualTo("mock_access_jwt_token");
        assertThat(response.getEmail()).isEqualTo("test@cinnamon.com");
        assertThat(response.getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.getStreakDays()).isEqualTo(1);

        // Xác minh DB thực sự được gọi để lưu user và khởi tạo UserXp
        verify(userRepository, times(1)).save(any(User.class));
        verify(userXpRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("REG-02: Đăng ký thất bại khi Email đã tồn tại")
    void register_EmailDaTonTai_NemBadRequestException() {
        // ARRANGE
        when(userRepository.existsByEmail("test@cinnamon.com")).thenReturn(true);

        // ACT (Thực thi và hứng lỗi ném ra)
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(validRequest);
        });

        // ASSERT (Kiểm chứng thông báo lỗi và đảm bảo không lưu rác vào DB)
        assertThat(exception.getMessage()).contains("Email đã được sử dụng");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("REG-03: Đăng ký thất bại khi Tên đăng nhập (username) đã tồn tại")
    void register_UsernameDaTonTai_NemBadRequestException() {
        // ARRANGE
        when(userRepository.existsByEmail("test@cinnamon.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // ACT
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(validRequest);
        });

        // ASSERT
        assertThat(exception.getMessage()).contains("Tên đăng nhập đã tồn tại");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("REG-04: Đăng ký thất bại khi nhập sai mã OTP")
    void register_SaiMaOtp_NemBadRequestException() {
        // ARRANGE
        when(userRepository.existsByEmail("test@cinnamon.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(otpTokenService.verify("mock_jwt_otp_token", "test@cinnamon.com", "123456"))
                .thenReturn(OtpResult.WRONG);

        // ACT
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(validRequest);
        });

        // ASSERT
        assertThat(exception.getMessage()).contains("Mã OTP không chính xác");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("REG-05: Đăng ký thất bại khi mã OTP đã hết hạn (quá 5 phút)")
    void register_OtpHetHan_NemBadRequestException() {
        // ARRANGE
        when(userRepository.existsByEmail("test@cinnamon.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(otpTokenService.verify("mock_jwt_otp_token", "test@cinnamon.com", "123456"))
                .thenReturn(OtpResult.EXPIRED);

        // ACT
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(validRequest);
        });

        // ASSERT
        assertThat(exception.getMessage()).contains("Mã OTP đã hết hạn");
        verify(userRepository, never()).save(any());
    }
}
