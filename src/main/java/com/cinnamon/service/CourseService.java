package com.cinnamon.service;

import com.cinnamon.dto.request.CourseRequest;
import com.cinnamon.dto.response.CourseResponse;
import com.cinnamon.entity.Course;
import com.cinnamon.enums.CourseLevel;
import com.cinnamon.enums.Role;
import com.cinnamon.entity.User;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.CourseRepository;
import com.cinnamon.repository.LessonRepository;
import com.cinnamon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CourseService - Dịch vụ quản lý các khóa học tiếng Anh chuẩn CEFR (A1, A2, B1, B2).
 * Đảm nhiệm:
 * - Lấy danh sách khóa học công khai cho học viên (có lọc theo Level và phân trang).
 * - Lấy danh sách khóa học do giáo viên quản lý.
 * - Tạo mới, cập nhật, xuất bản (publish/draft) và xóa khóa học kèm kiểm tra quyền sở hữu.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final LessonService lessonService;

    /**
     * Lấy danh sách tất cả các khóa học đã được xuất bản (isPublished = true) phục vụ học viên.
     *
     * @param level    cấp độ CEFR (A1, A2, B1, B2) cần lọc (nếu null sẽ lấy tất cả cấp độ)
     * @param pageable thông tin phân trang và sắp xếp
     * @return Trang danh sách CourseResponse
     */
    public Page<CourseResponse> getAllCourses(CourseLevel level, Pageable pageable) {
        Page<Course> page = (level != null)
                ? courseRepository.findByLevelAndIsPublishedTrue(level, pageable)
                : courseRepository.findByIsPublishedTrue(pageable);

        return page.map(this::toResponse);
    }

    /**
     * Xem thông tin chi tiết một khóa học theo ID.
     *
     * @param id mã định danh của khóa học
     * @return CourseResponse chứa thông tin khóa học và số lượng bài học
     */
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với id: " + id));
        return toResponse(course);
    }

    /**
     * Tạo mới một khóa học do giáo viên hoặc admin đứng tên.
     *
     * @param request      chứa tiêu đề, mô tả, cấp độ CEFR, thumbnail URL, trạng thái publish
     * @param teacherEmail email của giáo viên đang tạo khóa học
     * @return CourseResponse của khóa học vừa tạo
     */
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

    /**
     * Lấy danh sách các khóa học do một giáo viên cụ thể phụ trách.
     * Nếu người dùng là ADMIN thì trả về tất cả các khóa học trong hệ thống.
     *
     * @param teacherEmail email của giáo viên/admin
     * @return Danh sách CourseResponse
     */
    public List<CourseResponse> getCoursesByTeacher(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên không tồn tại"));

        List<Course> list = (teacher.getRole() == Role.ADMIN)
                ? courseRepository.findAll()
                : courseRepository.findByTeacherId(teacher.getId());

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Cập nhật thông tin khóa học (tiêu đề, mô tả, cấp độ, ảnh bìa, xuất bản).
     * Yêu cầu người thực hiện phải là chính giáo viên tạo ra khóa học đó hoặc là ADMIN.
     *
     * @param id        ID khóa học cần sửa
     * @param request   dữ liệu mới cần cập nhật
     * @param userEmail email của người đang thao tác
     * @return CourseResponse sau khi lưu
     */
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

    /**
     * Xóa một khóa học khỏi hệ thống.
     * Tự động xóa liên hoàn (Cascade) toàn bộ bài học, từ vựng, bài quiz và tiến trình học viên liên quan.
     *
     * @param id        ID khóa học cần xóa
     * @param userEmail email của người đang thao tác (phải là chủ sở hữu hoặc ADMIN)
     */
    @Transactional
    public void deleteCourse(Long id, String userEmail) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));

        validateCourseOwnership(course, userEmail);

        // Xoá toàn bộ bài học (kèm từ vựng, quiz, tiến độ) trước khi xoá khóa học
        lessonService.deleteLessonsByCourseId(id);
        courseRepository.delete(course);
    }

    /**
     * Kiểm tra quyền sở hữu khóa học: Giáo viên chỉ được sửa/xóa khóa học của chính mình.
     * Tài khoản ADMIN có toàn quyền bỏ qua kiểm tra này.
     */
    private void validateCourseOwnership(Course course, String userEmail) {
        if (userEmail == null) return;
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || user.getRole() == Role.ADMIN) return;

        if (course.getTeacher() != null && !course.getTeacher().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa khóa học của giáo viên khác");
        }
    }

    /**
     * Chuyển đổi đối tượng Course sang DTO CourseResponse, tính toán kèm tổng số bài học hiện có.
     */
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
                .totalLessons(lessonsCount)
                .build();
    }
}
