package com.cinnamon.repository;

import com.cinnamon.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    List<UserProgress> findByUserId(Long userId);
    List<UserProgress> findByLessonId(Long lessonId);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user.id = :userId AND up.isCompleted = true")
    long countCompletedByUserId(Long userId);

    @Query("SELECT COUNT(DISTINCT up.user.id) FROM UserProgress up WHERE up.lesson.id IN :lessonIds")
    long countDistinctStudentsByLessonIds(java.util.Collection<Long> lessonIds);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.lesson.id IN :lessonIds AND up.isCompleted = true")
    long countCompletedByLessonIds(java.util.Collection<Long> lessonIds);

    @Query("SELECT AVG(up.score) FROM UserProgress up WHERE up.lesson.id IN :lessonIds AND up.score IS NOT NULL")
    Double averageScoreByLessonIds(java.util.Collection<Long> lessonIds);
}
