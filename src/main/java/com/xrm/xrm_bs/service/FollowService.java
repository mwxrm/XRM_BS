package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.UserPublicResponse;
import com.xrm.xrm_bs.entity.User;
import com.xrm.xrm_bs.entity.UserFollow;
import com.xrm.xrm_bs.repository.UserFollowRepository;
import com.xrm.xrm_bs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 关注用户
    @Transactional
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new RuntimeException("不能关注自己");
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return;
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        followRepository.save(follow);

        User follower = userRepository.findById(followerId).orElse(null);
        if (follower != null) {
            String content = follower.getUsername() + " 关注了你";
            notificationService.createNotification(
                    followeeId,
                    "follow",
                    followerId,
                    followerId,
                    "user",
                    content
            );
        }
    }


    // 取消关注
    @Transactional
    public void unfollowUser(Long followerId, Long followeeId) {
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    // 检查关注状态
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    // 获取关注数
    @Transactional(readOnly = true)
    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    // 获取粉丝数
    @Transactional(readOnly = true)
    public long getFollowersCount(Long userId) {
        return followRepository.countByFolloweeId(userId);
    }

    // 获取关注列表（分页）
    @Transactional(readOnly = true)
    public Page<UserPublicResponse> getFollowingList(Long userId, Pageable pageable) {
        Page<Long> followeeIdsPage = followRepository.findFolloweeIdsByFollowerId(userId, pageable);
        List<Long> ids = followeeIdsPage.getContent();
        Map<Long, User> userMap = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<UserPublicResponse> responses = ids.stream()
                .map(id -> {
                    User u = userMap.get(id);
                    return u != null ? new UserPublicResponse(u.getId(), u.getUsername(), u.getRole(), u.getCreatedAt()) : null;
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, followeeIdsPage.getTotalElements());
    }

    // 获取粉丝列表（分页）
    @Transactional(readOnly = true)
    public Page<UserPublicResponse> getFollowersList(Long userId, Pageable pageable) {
        Page<Long> followerIdsPage = followRepository.findFollowerIdsByFolloweeId(userId, pageable);
        List<Long> ids = followerIdsPage.getContent();
        Map<Long, User> userMap = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<UserPublicResponse> responses = ids.stream()
                .map(id -> {
                    User u = userMap.get(id);
                    return u != null ? new UserPublicResponse(u.getId(), u.getUsername(), u.getRole(), u.getCreatedAt()) : null;
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, followerIdsPage.getTotalElements());
    }

    // 批量检查关注状态
    @Transactional(readOnly = true)
    public Map<Long, Boolean> batchCheckFollowing(Long followerId, List<Long> followeeIds) {
        List<Long> existing = followRepository.findFolloweeIdsByFollowerIdAndFolloweeIdIn(followerId, followeeIds);
        return followeeIds.stream().collect(Collectors.toMap(id -> id, existing::contains));
    }
}