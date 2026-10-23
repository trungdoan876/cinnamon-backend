package com.cinnamon.service;

import com.cinnamon.dto.request.LoginRequest;
import com.cinnamon.dto.request.RegisterRequest;
import com.cinnamon.dto.request.SendOtpRequest;
import com.cinnamon.dto.response.AuthResponse;
import com.cinnamon.dto.response.UserResponse;
import com.cinnamon.entity.EmailOtp;
import com.cinnamon.entity.Role;
import com.cinnamon.entity.User;
import com.cinnamon.entity.UserXp;
import com.cinnamon.exception.BadRequestException;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.EmailOtpRepository;
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
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserXpRepository userXpRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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
        consumeOtp(email, request.getOtp());

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

        // Cập nhật streak logic
        LocalDate today = LocalDate.now();
        if (user.getLastLoginDate() == null) {
            user.setStreakDays(1);
        } else if (user.getLastLoginDate().plusDays(1).isEqual(today)) {
            user.setStreakDays(user.getStreakDays() + 1);
        } else if (user.getLastLoginDate().isBefore(today.minusDays(1))) {
            user.setStreakDays(1); // Mất chuỗi streak
        }
        user.setLastLoginDate(today);
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
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Kiểm tra mã OTP của email. Ném lỗi nếu không tồn tại / hết hạn / sai mã.
     * Nếu hợp lệ thì đánh dấu đã sử dụng để không thể dùng lại.
     */
    private void consumeOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedOtp = otp == null ? "" : otp.trim();

        EmailOtp otpRecord = emailOtpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã OTP cho email này. Vui lòng bấm gửi lại mã"));

        if (otpRecord.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới");
        }

        if (!otpRecord.getOtp().equals(normalizedOtp)) {
            throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại");
        }

        otpRecord.setUsed(true);
        emailOtpRepository.save(otpRecord);
    }

    @Transactional
    public void sendOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        boolean forRegister = "REGISTER".equalsIgnoreCase(request.getPurpose());

        if (forRegister && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng email khác");
        }

        // Tạo mã OTP ngẫu nhiên 6 chữ số
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));

        // Lưu vào bảng email_otps với thời hạn 5 phút
        EmailOtp emailOtp = EmailOtp.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        emailOtpRepository.save(emailOtp);

        // Gửi email thật qua SMTP
        emailService.sendOtpEmail(email, otp, forRegister ? "REGISTER" : "LOGIN");
    }
}
