package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.RecentView;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RecentViewRepository extends JpaRepository<RecentView, Long> {
    Page<RecentView> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);
    void deleteByUserIdAndItemIdAndItemType(Long userId, Long itemId, String itemType);

    boolean existsByUserIdAndItemIdAndItemType(Long userId, Long itemId, String itemType);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}