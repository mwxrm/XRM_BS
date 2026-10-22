package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.Setting;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> ,JpaSpecificationExecutor<Setting> {

    @Query("SELECT s FROM Setting s LEFT JOIN FETCH s.author LEFT JOIN FETCH s.tags WHERE s.id = :id")
    Optional<Setting> findByIdWithAll(@Param("id") Long id);

    // 根据ID查询未删除的设定
    @Query("SELECT s FROM Setting s WHERE s.id = :id AND s.deleted = false")
    Optional<Setting> findByIdNotDeleted(@Param("id") Long id);

    // 查询已发布的设定（未删除）
    Page<Setting> findByStatusAndDeletedFalse(Setting.SettingStatus status, Pageable pageable);
    List<Setting> findByStatusAndDeletedFalseOrderByCreatedAtDesc(Setting.SettingStatus status);

    // 查询用户的所有设定（未删除）
    List<Setting> findByAuthorIdAndDeletedFalse(Long authorId);

    // 查询用户的草稿设定（未删除）
    List<Setting> findByAuthorIdAndStatusAndDeletedFalse(Long authorId, Setting.SettingStatus status);

    // 根据父ID和状态查询子设定（未删除且已发布）
    Page<Setting> findByParentSettingIdAndStatusAndDeletedFalse(Long parentId, Setting.SettingStatus status, Pageable pageable);

    List<Setting> findAllByStatusAndDeletedFalse(Setting.SettingStatus status);

    // 软删除：直接更新 deleted 字段
    @Modifying
    @Query("UPDATE Setting s SET s.deleted = true WHERE s.id = :id")
    void softDeleteById(@Param("id") Long id);

    Page<Setting> findByAuthorIdAndStatusAndDeletedFalse(Long authorId, Setting.SettingStatus status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Setting s SET s.viewCount = s.viewCount + 1 WHERE s.id = :id")
    void incrementViewCount(@Param("id") Long id);


    @Query("SELECT COALESCE(SUM(s.likeCount), 0) FROM Setting s WHERE s.author.id = :authorId AND s.deleted = false")
    long sumLikeCountByAuthorId(@Param("authorId") Long authorId);


    @Query("SELECT COUNT(s) FROM Setting s WHERE s.parentSetting.id = :parentId AND s.status = :status AND s.deleted = false")
    long countByParentSettingIdAndStatus(@Param("parentId") Long parentId, @Param("status") Setting.SettingStatus status);

}