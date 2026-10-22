package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.UserBehaviorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserBehaviorLogRepository extends JpaRepository<UserBehaviorLog, Long> {

    @Query("SELECT COUNT(b) FROM UserBehaviorLog b WHERE b.itemId = :itemId " +
            "AND b.itemType = :itemType AND b.action = 'RECOMMEND_CLICK' " +
            "AND b.actionTime > :since")
    long countRecentRecommendClicks(@Param("itemId") Long itemId,
                                    @Param("itemType") String itemType,
                                    @Param("since") LocalDateTime since);


}