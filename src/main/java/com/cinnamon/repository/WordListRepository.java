package com.cinnamon.repository;

import com.cinnamon.entity.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordListRepository extends JpaRepository<WordList, Long> {
    List<WordList> findByUserIdOrderByIdDesc(Long userId);

    Optional<WordList> findByIdAndUserId(Long id, Long userId);
}

