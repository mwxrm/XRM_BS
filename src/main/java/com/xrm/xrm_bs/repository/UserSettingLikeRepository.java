package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.UserSettingLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSettingLikeRepository extends JpaRepository<UserSettingLike, Long> {
    boolean existsByUserIdAndSettingId(Long userId, Long settingId);
    void deleteByUserIdAndSettingId(Long userId, Long settingId);


    @Query("SELECT COUNT(usl) FROM UserSettingLike usl WHERE usl.settingId = :settingId")
    long countBySettingId(@Param("settingId") Long settingId);


}