package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id")
    void decrementLikeCount(@Param("id") Long id);

    Page<Comment> findByTargetTypeAndTargetIdAndParentIdIsNullAndStatus(String targetType, Long targetId, int status, Pageable pageable);
    List<Comment> findByParentIdInAndStatus(List<Long> parentIds, int status);


    // 统计一级评论总数（排除软删除）
    long countByTargetTypeAndTargetIdAndParentIdIsNullAndStatus(String targetType, Long targetId, int status);

    // 统计某个时间点之后的评论数量（排除软删除）- 用于计算页码
    int countByTargetTypeAndTargetIdAndStatusAndCreatedAtAfter(String targetType, Long targetId, int status, LocalDateTime createdAt);

    // 如果需要过滤软删除，可以再加一个方法
    List<Comment> findByParentIdAndStatus(Long parentId, int status);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.targetType = :targetType AND c.targetId = :targetId AND c.status = 1")
    long countByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);


}