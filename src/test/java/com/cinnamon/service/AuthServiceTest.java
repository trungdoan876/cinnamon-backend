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

import org.springframework.security.authentication.AuthenticationManager;

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
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@cinnamon.com");
        validRequest.setPassword("Password123@");
        validRequest.setFullName("Test User");
        validRequest.setOtp("123456");
        validRequest.setOtpToken("mock_jwt_otp_token");

        // Tạo sẵn User mẫu đại diện cho tài khoản
        // đã có trong Database để login
        existingUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testlogin@cinnamon.com")
                .password("hashed_password")
                .role(Role.STUDENT)
                .isActive(true)
                .streakDays(1)
                .build();
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

    // LUỒNG ĐĂNG NHẬP
    @Test
    @DisplayName("LOG-01: ĐĂNG NHẬP THÀNH CÔNG")
    void login_Success() {
        // Arrange
        when(userRepository.findByEmail("testlogin@cinnamon.com"))
                .thenReturn(Optional.of(existingUser));
        // kiểm tra pass luôn đúng
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(true);

        // sinh token giả lập
        when(jwtService.generateToken(any()))
                .thenReturn("mock_login_jwt_token");
        // Act
        LoginRequest loginReq = new LoginRequest("testlogin@cinnamon.com", "hashed_password");
        AuthResponse response = authService.login(loginReq);

        // Assert - kiểm tra lại
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock_login_jwt_token");
        // hệ thống có gọi save() để lưu chuỗi
        // streak mới vào DB
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("LOG-02: Không tìm thấy email")
    void login_NotFoundEmail() {
        // arrange
        when(userRepository.findByEmail("unknown@cinnamon.com"))
                .thenReturn(Optional.empty());
        // act
        LoginRequest loginReq = new LoginRequest("unknown@cinnamon.com", "Password123@");
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.login(loginReq);
        });
        // assert
        assertThat(exception.getMessage()).contains("Không tìm thấy tài khoản với email này");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("LOG-03: Tài khoản bị khóa")
    void loginAccountLock() {
        // arrange
        existingUser.setIsActive(false);
        when(userRepository.findByEmail("testAccountLock@cinnamon.com"))
                .thenReturn(Optional.of(existingUser));
        // act
        LoginRequest loginReq = new LoginRequest("testAccountLock@cinnamon.com", "Password123@");
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.login(loginReq);
        });
        // assert
        assertThat(exception.getMessage()).contains("Tài khoản của bạn đã bị khóa");
        // khi đăng nhập thất bại, ko lưu vào db
        // kiểm tra ko được gọi hàm save và ko sinh mã token
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));

    }

    @Test
    @DisplayName("LOG-04: Đăng nhập sai mật khẩu")
    void login_PasswordFault() {
        // arrange
        when(userRepository.findByEmail("testPasssword@cinnamon.com"))
                .thenReturn(Optional.of(existingUser));
        // gán sai pass
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);
        // act
        LoginRequest loginReq = new LoginRequest("testPasssword@cinnamon.com", "WrongPassword999@");
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.login(loginReq);
        });
        // assert
        assertThat(exception.getMessage()).contains("Email hoặc mật khẩu không chính xác");
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

}
