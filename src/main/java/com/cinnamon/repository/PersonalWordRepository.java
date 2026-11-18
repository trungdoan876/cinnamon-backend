package com.cinnamon.repository;

import com.cinnamon.entity.PersonalWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalWordRepository extends JpaRepository<PersonalWord, Long> {
    List<PersonalWord> findByUserIdOrderByIdDesc(Long userId);

    List<PersonalWord> findByUserIdAndListIdOrderByIdDesc(Long userId, Long listId);

    Optional<PersonalWord> findByIdAndUserId(Long id, Long userId);

    long countByListId(Long listId);

    long countByListIdAndMastered(Long listId, Boolean mastered);

    void deleteByListId(Long listId);
}
