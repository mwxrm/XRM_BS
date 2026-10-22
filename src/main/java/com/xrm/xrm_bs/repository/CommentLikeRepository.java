package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);

    List<CommentLike> findByCommentIdInAndUserId(List<Long> commentIds, Long userId);
}