package com.cinnamon.service;

import com.cinnamon.dto.response.AdminStatsResponse;
import com.cinnamon.dto.response.UserResponse;
import com.cinnamon.enums.Role;
import com.cinnamon.entity.User;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminService - Dịch vụ quản trị hệ thống dành riêng cho Quản trị viên (ADMIN).
 * Cung cấp các chức năng:
 * - Thống kê toàn cảnh hệ thống (số lượng người dùng, khóa học, bài học, lượt hoàn thành).
 * - Tìm kiếm, phân trang và quản lý danh sách người dùng.
 * - Phân quyền tài khoản (ROLE) và kích hoạt/khóa tài khoản người dùng.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final UserProgressRepository userProgressRepository;

    /**
     * Lấy các chỉ số thống kê tổng quan của hệ thống phục vụ Admin Dashboard.
     *
     * @return AdminStatsResponse chứa tổng số user, student, teacher, admin, courses, lessons, quizzes.
     */
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalStudents(userRepository.countByRole(Role.STUDENT))
                .totalTeachers(userRepository.countByRole(Role.TEACHER))
                .totalAdmins(userRepository.countByRole(Role.ADMIN))
                .totalCourses(courseRepository.count())
                .totalLessons(lessonRepository.count())
                .totalQuizzes(quizRepository.count())
                .totalCompletedLessons(userProgressRepository.count())
                .build();
    }

    /**
     * Tìm kiếm và phân trang danh sách người dùng theo vai trò (Role) và từ khóa tìm kiếm (Username/Email/Tên).
     *
     * @param role     vai trò cần lọc (STUDENT, TEACHER, ADMIN), có thể để null nếu lấy tất cả
     * @param keyword  từ khóa tìm kiếm theo tên hoặc email
     * @param pageable thông tin phân trang (trang số mấy, số phần tử mỗi trang, sắp xếp)
     * @return Trang danh sách UserResponse
     */
    public Page<UserResponse> searchUsers(Role role, String keyword, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Page<User> page = userRepository.searchUsers(role, cleanKeyword, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Thay đổi vai trò (Role) của một tài khoản người dùng.
     *
     * @param userId  ID của người dùng cần thay đổi
     * @param newRole Vai trò mới (STUDENT, TEACHER, ADMIN)
     * @return UserResponse thông tin người dùng sau khi cập nhật
     */
    @Transactional
    public UserResponse updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setRole(newRole);
        return toResponse(userRepository.save(user));
    }

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (kích hoạt/hủy kích hoạt).
     *
     * @param userId   ID của người dùng
     * @param isActive true = mở khóa cho phép đăng nhập, false = khóa tài khoản
     * @return UserResponse thông tin người dùng sau khi cập nhật
     */
    @Transactional
    public UserResponse updateUserStatus(Long userId, Boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setIsActive(isActive);
        return toResponse(userRepository.save(user));
    }

    /**
     * Chuyển đổi đối tượng thực thể User (Entity) sang DTO UserResponse để trả về client an toàn (ẩn mật khẩu).
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
