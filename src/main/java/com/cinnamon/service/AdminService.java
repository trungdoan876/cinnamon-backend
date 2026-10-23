package com.cinnamon.service;

import com.cinnamon.dto.response.AdminStatsResponse;
import com.cinnamon.dto.response.UserResponse;
import com.cinnamon.entity.Role;
import com.cinnamon.entity.User;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final UserProgressRepository userProgressRepository;

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

    public Page<UserResponse> searchUsers(Role role, String keyword, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Page<User> page = userRepository.searchUsers(role, cleanKeyword, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setRole(newRole);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, Boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setIsActive(isActive);
        return toResponse(userRepository.save(user));
    }

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
                .createdAt(user.getCreatedAt())
                .build();
    }
}
