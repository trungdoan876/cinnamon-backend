package com.cinnamon.service;

import com.cinnamon.dto.request.CourseRequest;
import com.cinnamon.dto.response.CourseResponse;
import com.cinnamon.entity.Course;
import com.cinnamon.entity.CourseLevel;
import com.cinnamon.entity.User;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.CourseRepository;
import com.cinnamon.repository.LessonRepository;
import com.cinnamon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final LessonService lessonService;

    public Page<CourseResponse> getAllCourses(CourseLevel level, Pageable pageable) {
        Page<Course> page = (level != null)
                ? courseRepository.findByLevelAndIsPublishedTrue(level, pageable)
                : courseRepository.findByIsPublishedTrue(pageable);

        return page.map(this::toResponse);
    }

    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với id: " + id));
        return toResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên không tồn tại"));

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .level(request.getLevel())
                .thumbnailUrl(request.getThumbnailUrl())
                .teacher(teacher)
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
                .build();

        return toResponse(courseRepository.save(course));
    }

    public List<CourseResponse> getCoursesByTeacher(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên không tồn tại"));

        List<Course> list = (teacher.getRole() == com.cinnamon.entity.Role.ADMIN)
                ? courseRepository.findAll()
                : courseRepository.findByTeacherId(teacher.getId());

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request, String userEmail) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));

        validateCourseOwnership(course, userEmail);

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setLevel(request.getLevel());
        if (request.getThumbnailUrl() != null) {
            course.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getIsPublished() != null) {
            course.setIsPublished(request.getIsPublished());
        }

        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id, String userEmail) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));

        validateCourseOwnership(course, userEmail);

        // Xoá toàn bộ bài học (kèm từ vựng, quiz, tiến độ) trước khi xoá khóa học
        lessonService.deleteLessonsByCourseId(id);
        courseRepository.delete(course);
    }

    private void validateCourseOwnership(Course course, String userEmail) {
        if (userEmail == null) return;
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || user.getRole() == com.cinnamon.entity.Role.ADMIN) return;

        if (course.getTeacher() != null && !course.getTeacher().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền chỉnh sửa khóa học của giáo viên khác");
        }
    }

    private CourseResponse toResponse(Course course) {
        int lessonsCount = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()).size();

        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .level(course.getLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .teacherId(course.getTeacher() != null ? course.getTeacher().getId() : null)
                .teacherName(course.getTeacher() != null ? course.getTeacher().getFullName() : "Cinnamon Team")
                .isPublished(course.getIsPublished())
                .createdAt(course.getCreatedAt())
                .totalLessons(lessonsCount)
                .build();
    }
}
