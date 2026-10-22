package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndIsReadFalse(Long userId);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.id IN :ids")
    void markAsRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllAsRead(@Param("userId") Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);

    // 根据类型、目标类型、目标ID查询
    List<Notification> findByUserIdAndTypeAndTargetTypeAndTargetIdOrderByCreatedAtDesc(
            Long userId, String type, String targetType, Long targetId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.targetType = :targetType AND n.targetId = :targetId AND n.type IN :types ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndTargetTypeAndTargetIdAndTypeInOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("types") List<String> types);

    List<Notification> findByUserIdAndTargetIdInAndTypeOrderByCreatedAtDesc(
            Long userId, List<Long> targetIds, String type);

    /**
     * 删除指定用户对同一目标、同类型的旧通知（用于防刷）
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.type = :type AND n.targetId = :targetId AND n.sourceUserId = :sourceUserId")
    void deleteDuplicateNotifications(@Param("userId") Long userId,
                                      @Param("type") String type,
                                      @Param("targetId") Long targetId,
                                      @Param("sourceUserId") Long sourceUserId);
}














