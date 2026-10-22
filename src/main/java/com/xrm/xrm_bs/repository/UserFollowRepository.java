package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    // 关注数
    long countByFollowerId(Long followerId);

    // 粉丝数
    long countByFolloweeId(Long followeeId);

    // 关注列表（我关注的人）
    @Query("SELECT uf.followeeId FROM UserFollow uf WHERE uf.followerId = :userId ORDER BY uf.createdAt DESC")
    Page<Long> findFolloweeIdsByFollowerId(@Param("userId") Long userId, Pageable pageable);


    // 粉丝列表（关注我的人）
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followeeId = :userId ORDER BY uf.createdAt DESC")
    Page<Long> findFollowerIdsByFolloweeId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT uf.followeeId FROM UserFollow uf WHERE uf.followerId = :followerId AND uf.followeeId IN :followeeIds")
    List<Long> findFolloweeIdsByFollowerIdAndFolloweeIdIn(@Param("followerId") Long followerId, @Param("followeeIds") List<Long> followeeIds);

    @Query("SELECT uf.followeeId FROM UserFollow uf WHERE uf.followerId = :followerId")
    List<Long> findFolloweeIdsByFollowerId(@Param("followerId") Long followerId);

}

