package com.cinnamon.repository;

import com.cinnamon.entity.Course;
import com.cinnamon.entity.CourseLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findByIsPublishedTrue(Pageable pageable);
    Page<Course> findByLevelAndIsPublishedTrue(CourseLevel level, Pageable pageable);
    List<Course> findByTeacherId(Long teacherId);
}
