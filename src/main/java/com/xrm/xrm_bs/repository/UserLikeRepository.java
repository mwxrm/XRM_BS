package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.UserLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLikeRepository extends JpaRepository<UserLike, Long> {
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);
    void deleteByUserIdAndArticleId(Long userId, Long articleId);


    @Query("SELECT COUNT(ul) FROM UserLike ul WHERE ul.articleId = :articleId")
    long countByArticleId(@Param("articleId") Long articleId);


}