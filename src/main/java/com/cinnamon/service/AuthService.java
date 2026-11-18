package com.cinnamon.service;

import com.cinnamon.dto.request.LoginRequest;
import com.cinnamon.dto.request.RegisterRequest;
import com.cinnamon.dto.request.SendOtpRequest;
import com.cinnamon.dto.response.AuthResponse;
import com.cinnamon.dto.response.UserResponse;
import com.cinnamon.enums.Role;
import com.cinnamon.entity.User;
import com.cinnamon.entity.UserXp;
import com.cinnamon.exception.BadRequestException;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.UserRepository;
import com.cinnamon.repository.UserXpRepository;
import com.cinnamon.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * AuthService - Dịch vụ xác thực và phân quyền tài khoản người dùng.
 * Đảm nhiệm:
 * - Đăng ký tài khoản học viên mới kèm xác thực mã OTP gửi về email.
 * - Đăng nhập tài khoản, xác thực mật khẩu qua BCrypt và cấp phát JWT token.
 * - Tự động cập nhật chuỗi streak học tập hàng ngày của học viên.
 * - Tạo và gửi mã xác thực OTP 6 số qua email thật (Gmail SMTP).
 * - Cung cấp thông tin tài khoản hiện tại (me).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserXpRepository userXpRepository;
    private final OtpTokenService otpTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Đăng ký tài khoản học viên mới vào hệ thống.
     * Quy trình:
     * 1. Kiểm tra tính duy nhất của email và username.
     * 2. Xác thực mã OTP người dùng nhập với mã đã gửi tới email.
     * 3. Mã hóa mật khẩu bảo mật bằng BCrypt.
     * 4. Tạo bản ghi User với vai trò STUDENT và tạo avatar mặc định.
     * 5. Khởi tạo bản ghi UserXp (cấp độ 1, 0 XP).
     * 6. Cấp phát JWT token để người dùng tự động đăng nhập ngay.
     *
     * @param request chứa username, email, password, fullName, otp
     * @return AuthResponse chứa JWT Token và thông tin cơ bản của người dùng
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại");
        }

        // Bắt buộc phải xác thực đúng mã OTP đã gửi tới email trước khi tạo tài khoản
        consumeOtp(email, request.getOtp(), request.getOtpToken());

        User user = User.builder()
                .username(request.getUsername())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.STUDENT)
                .isActive(true)
                .streakDays(1)
                .lastLoginDate(LocalDate.now())
                .avatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=" + request.getUsername())
                .build();

        user = userRepository.save(user);

        // Khởi tạo XP ban đầu
        UserXp userXp = UserXp.builder()
                .user(user)
                .totalXp(0)
                .level(1)
                .weeklyXp(0)
                .build();
        userXpRepository.save(userXp);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getRealUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .streakDays(user.getStreakDays())
                .build();
    }

    /**
     * Đăng nhập hệ thống bằng email và mật khẩu.
     * Quy trình:
     * 1. Tìm tài khoản theo email, kiểm tra xem tài khoản có bị ban/khóa không.
     * 2. So khớp mật khẩu với hàm băm BCrypt trong database.
     * 3. Ủy quyền xác thực cho Spring Security AuthenticationManager.
     * 4. Gọi phương thức domain user.recordLogin(...) để cập nhật streak học tập.
     * 5. Sinh JWT Token mới trả về cho client.
     *
     * @param request chứa email và password
     * @return AuthResponse chứa JWT Token và thông tin người dùng
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email này"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ ban quản trị.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Email hoặc mật khẩu không chính xác");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Cập nhật streak trong service khi đăng nhập
        updateLoginStreak(user);
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getRealUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .streakDays(user.getStreakDays())
                .build();
    }

    /**
     * Lấy thông tin hồ sơ của người dùng hiện đang đăng nhập (dựa trên email giải mã từ JWT).
     *
     * @param email email của tài khoản đang đăng nhập
     * @return UserResponse chứa thông tin hồ sơ
     */
    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getRealUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .streakDays(user.getStreakDays())
                .lastLoginDate(user.getLastLoginDate())
                .build();
    }

    /**
     * Xác thực mã OTP bằng otpToken (JWT) thông qua {@link OtpTokenService}, không cần lưu gì ở server.
     * Kiểm tra: token hợp lệ (chữ ký, đúng email), chưa hết hạn 5 phút và mã khớp chính xác.
     *
     * @param email    email nhận mã
     * @param otp      mã 6 số người dùng nhập
     * @param otpToken token do sendOtp trả về
     */
    private void consumeOtp(String email, String otp, String otpToken) {
        switch (otpTokenService.verify(otpToken, email, otp)) {
            case OK -> { }
            case INVALID -> throw new BadRequestException("Phiên xác thực OTP không hợp lệ. Vui lòng bấm gửi lại mã");
            case EXPIRED -> throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới");
            case WRONG -> throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại");
        }
    }

    /**
     * Sinh mã OTP 6 chữ số, gửi qua email và trả về otpToken (JWT) để client giữ lại khi đăng ký.
     * Thời hạn hiệu lực là 5 phút.
     *
     * @param request chứa email và mục đích (REGISTER / LOGIN / FORGOT_PASSWORD)
     * @return otpToken cần gửi kèm mã OTP ở bước đăng ký
     */
    private void updateLoginStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = user.getLastLoginDate();
        if (lastLogin == null) {
            user.setStreakDays(1);
        } else if (lastLogin.plusDays(1).isEqual(today)) {
            user.setStreakDays((user.getStreakDays() != null ? user.getStreakDays() : 0) + 1);
        } else if (lastLogin.isBefore(today.minusDays(1))) {
            user.setStreakDays(1);
        }
        user.setLastLoginDate(today);
    }

    public String sendOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        boolean forRegister = "REGISTER".equalsIgnoreCase(request.getPurpose());

        if (forRegister && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng email khác");
        }

        OtpTokenService.Issued issued = otpTokenService.issue(email);
        emailService.sendOtpEmail(email, issued.otp(), forRegister ? "REGISTER" : "LOGIN");
        return issued.token();
    }
}
