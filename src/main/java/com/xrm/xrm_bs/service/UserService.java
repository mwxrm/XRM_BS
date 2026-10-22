package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.entity.Tag;
import com.xrm.xrm_bs.entity.User;
import com.xrm.xrm_bs.dto.RegisterRequest;
import com.xrm.xrm_bs.dto.UserResponse;
import com.xrm.xrm_bs.entity.UserBlockedTag;
import com.xrm.xrm_bs.entity.UserPreferredTag;
import com.xrm.xrm_bs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 使用字段注入

    private final ArticleRepository articleRepository;

    private final SettingRepository settingRepository;

    private final UserPreferredTagRepository userPreferredTagRepository;

    private final TagRepository tagRepository;

    private final UserBlockedTagRepository userBlockedTagRepository;

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        // 创建新用户 - 暂时使用邮箱作为用户名，稍后会更改为 THY+ID
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        // 加密密码
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // 使用邮箱作为临时用户名（必须设置，因为数据库不允许null）
        user.setUsername("temp_" + System.currentTimeMillis());

        // 保存用户以获取ID
        user = userRepository.save(user);

        // 生成 THY+ID 格式的用户名
        String generatedUsername = "THY" + user.getId();
        user.setUsername(generatedUsername);

        // 更新用户
        user = userRepository.save(user);

        return user;
    }


    public User findByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }

    public User findByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("邮箱不存在: " + email));
    }

    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(pageable);
        }
        try {
            Long id = Long.parseLong(keyword.trim());
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                List<User> users = Collections.singletonList(userOpt.get());
                // 显式指定泛型类型为 User
                return new PageImpl<User>(users, pageable, 1);
            } else {
                return Page.empty(pageable);
            }
        } catch (NumberFormatException e) {
            return userRepository.findByUsernameContaining(keyword.trim(), pageable);
        }
    }


    public long getTotalLikes(Long userId) {
        // 统计该用户所有文章和设定的点赞总数
        long articleLikes = articleRepository.sumLikeCountByAuthorId(userId);
        long settingLikes = settingRepository.sumLikeCountByAuthorId(userId);
        return articleLikes + settingLikes;
    }

    @Transactional(readOnly = true)
    public List<Tag> getUserPreferredTags(Long userId) {
        return userPreferredTagRepository.findByUserId(userId).stream()
                .map(UserPreferredTag::getTag)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePreferredTags(Long userId, List<Long> tagIds) {
        if (tagIds.size() > 50) {
            throw new RuntimeException("最多只能选择50个喜好标签");
        }
        // 清除原有偏好
        userPreferredTagRepository.deleteByUserId(userId);
        // 添加新偏好
        List<Tag> tags = tagRepository.findAllById(tagIds);
        for (Tag tag : tags) {
            UserPreferredTag upt = new UserPreferredTag();
            upt.setUserId(userId);
            upt.setTag(tag);
            userPreferredTagRepository.save(upt);
        }
    }

    @Transactional(readOnly = true)
    public List<Tag> getBlockedTags(Long userId) {
        return userBlockedTagRepository.findByUserId(userId).stream()
                .map(UserBlockedTag::getTag)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBlockedTags(Long userId, List<Long> tagIds) {
        if (tagIds.size() > 50) {
            throw new RuntimeException("最多只能屏蔽50个标签");
        }
        // 清除原有屏蔽
        userBlockedTagRepository.deleteByUserId(userId);
        // 添加新屏蔽
        List<Tag> tags = tagRepository.findAllById(tagIds);
        for (Tag tag : tags) {
            UserBlockedTag ubt = new UserBlockedTag();
            ubt.setUserId(userId);
            ubt.setTag(tag);
            userBlockedTagRepository.save(ubt);
        }
    }
}