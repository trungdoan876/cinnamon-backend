package com.cinnamon.service;

import com.cinnamon.dto.request.ChangePasswordRequest;
import com.cinnamon.dto.request.UpdateProfileRequest;
import com.cinnamon.dto.response.UserResponse;
import com.cinnamon.entity.User;
import com.cinnamon.exception.BadRequestException;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService - Dịch vụ quản lý thông tin tài khoản cá nhân của người dùng.
 * Cung cấp:
 * - Xem thông tin hồ sơ cá nhân (Profile).
 * - Cập nhật họ tên và ảnh đại diện (Avatar).
 * - Đổi mật khẩu tài khoản với kiểm tra bảo mật (mật khẩu cũ phải đúng, mật khẩu mới không trùng mật khẩu cũ).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy thông tin chi tiết hồ sơ người dùng theo địa chỉ email.
     *
     * @param email email của người dùng
     * @return UserResponse chứa các thông tin tài khoản
     */
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return toResponse(user);
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân (Họ tên và Avatar URL).
     *
     * @param email   email của người dùng cần cập nhật
     * @param request chứa fullName và avatarUrl mới
     * @return UserResponse sau khi lưu vào database
     */
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return toResponse(userRepository.save(user));
    }

    /**
     * Đổi mật khẩu tài khoản người dùng.
     * Kiểm tra bảo mật:
     * 1. So khớp mật khẩu cũ với mật khẩu đã mã hóa trong database.
     * 2. Ngăn người dùng đặt mật khẩu mới trùng lặp với mật khẩu hiện tại.
     * 3. Mã hóa BCrypt mật khẩu mới trước khi lưu.
     *
     * @param email   email người dùng
     * @param request chứa oldPassword và newPassword
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Chuyển đổi Entity User sang DTO UserResponse an toàn (ẩn mật khẩu hash).
     */
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getRealUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .streakDays(user.getStreakDays())
                .lastLoginDate(user.getLastLoginDate())
                .build();
    }
}
