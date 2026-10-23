package com.cinnamon.repository;

import com.cinnamon.entity.UserXp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserXpRepository extends JpaRepository<UserXp, Long> {
    Optional<UserXp> findByUserId(Long userId);
    List<UserXp> findAllByOrderByTotalXpDesc(Pageable pageable);
    List<UserXp> findAllByOrderByWeeklyXpDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE UserXp ux SET ux.weeklyXp = 0")
    void resetWeeklyXp();
}
