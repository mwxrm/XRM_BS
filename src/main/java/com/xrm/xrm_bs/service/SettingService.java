package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.CreateSettingRequest;
import com.xrm.xrm_bs.dto.SettingResponse;
import com.xrm.xrm_bs.dto.UpdateSettingRequest;
import com.xrm.xrm_bs.entity.Setting;
import com.xrm.xrm_bs.entity.Tag;
import com.xrm.xrm_bs.entity.User;
import com.xrm.xrm_bs.entity.UserSettingLike;
import com.xrm.xrm_bs.repository.*;
import jakarta.persistence.criteria.Join;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class SettingService {
    private final SettingRepository settingRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final UserSettingLikeRepository userSettingLikeRepository;
    private final NotificationService notificationService;
    private final UserBehaviorLogService behaviorLogService;


    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Transactional
    public Setting createSetting(CreateSettingRequest request, Long authorId, Setting.SettingCategory category) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Setting setting = new Setting();
        setting.setTitle(request.getTitle());
        setting.setContent(request.getContent());
        setting.setCategory(category);  // 关键
        setting.setAuthor(author);
        setting.setStatus(Setting.SettingStatus.draft);


        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (String tagName : request.getTags()) {
                Tag tag = findOrCreateSettingTag(tagName, category);
                tags.add(tag);
            }
            setting.setTags(tags);
        }

        if (request.getParentSettingId() != null) {
            Setting parent = settingRepository.findById(request.getParentSettingId())
                    .orElseThrow(() -> new RuntimeException("引用的设定不存在"));
            if (parent.getStatus() != Setting.SettingStatus.published) {
                throw new RuntimeException("只能引用已发布的设定");
            }
            setting.setParentSetting(parent);
        }

        return settingRepository.save(setting);
    }

    private Tag findOrCreateSettingTag(String tagName, Setting.SettingCategory category) {
        return tagRepository.findByNameAndType(tagName, Tag.TagType.setting)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(tagName);
                    newTag.setType(Tag.TagType.setting);
                    newTag.setSettingCategory(category);
                    newTag.setUsageCount(0);
                    return tagRepository.save(newTag);
                });
    }

    @Transactional
    public Setting updateSetting(Long settingId, UpdateSettingRequest request, Long authorId) {
        Setting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new RuntimeException("设定不存在"));
        if (!setting.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("无权修改此设定");
        }

        // 只更新标题和内容，不更新分类
        setting.setTitle(request.getTitle());
        setting.setContent(request.getContent());



        if (request.getTags() != null) {
            Set<Tag> tags = new HashSet<>();
            for (String tagName : request.getTags()) {
                Tag tag = findOrCreateSettingTag(tagName, setting.getCategory()); // 使用设定现有的分类
                tags.add(tag);
            }
            setting.getTags().clear();
            setting.getTags().addAll(tags);
        }


        // 更新引用关系
        if (request.getParentSettingId() != null) {
            if (request.getParentSettingId().equals(settingId)) {
                throw new RuntimeException("不能引用设定自己");
            }
            Setting newParent = settingRepository.findById(request.getParentSettingId())
                    .orElseThrow(() -> new RuntimeException("引用的设定不存在"));
            if (newParent.getStatus() != Setting.SettingStatus.published) {
                throw new RuntimeException("只能引用已发布的设定");
            }
            if (isCircularReference(newParent, settingId)) {
                throw new RuntimeException("不能形成循环引用");
            }
            setting.setParentSetting(newParent);
        } else {
            setting.setParentSetting(null);
        }


        return settingRepository.save(setting);
    }


    @Transactional(readOnly = true)
    public Page<Setting> getPublishedSettings(Pageable pageable) {
        return settingRepository.findByStatusAndDeletedFalse(Setting.SettingStatus.published, pageable);
    }

    @Transactional
    public Setting publishSetting(Long settingId, Long userId) {
        Setting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new RuntimeException("设定不存在"));
        if (!setting.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权发布此设定");
        }
        setting.setStatus(Setting.SettingStatus.published);
        behaviorLogService.log(userId, settingId, "setting", "publish");
        return settingRepository.save(setting);
    }

    @Transactional
    public void likeSetting(Long userId, Long settingId) {
        if (userSettingLikeRepository.existsByUserIdAndSettingId(userId, settingId)) {
            throw new RuntimeException("已经点赞过");
        }
        UserSettingLike like = new UserSettingLike();
        like.setUserId(userId);
        like.setSettingId(settingId);
        userSettingLikeRepository.save(like);

        // 增加设定的点赞计数
        Setting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new RuntimeException("设定不存在"));
        setting.setLikeCount(setting.getLikeCount() + 1);
        settingRepository.save(setting);

        // 点赞后，给作者发送通知
        notificationService.createNotification(
                setting.getAuthor().getId(),
                "like_setting",
                userId,
                settingId,
                "setting",
                "赞了你的设定《" + setting.getTitle() + "》"
        );
    }

    @Transactional
    public void unlikeSetting(Long userId, Long settingId) {
        userSettingLikeRepository.deleteByUserIdAndSettingId(userId, settingId);
        Setting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new RuntimeException("设定不存在"));
        setting.setLikeCount(setting.getLikeCount() - 1);
        settingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public boolean isUserLiked(Long userId, Long settingId) {
        return userSettingLikeRepository.existsByUserIdAndSettingId(userId, settingId);
    }

    @Transactional(readOnly = true)
    public List<Setting> getMySettings(Long userId) {
        return settingRepository.findByAuthorIdAndDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<Setting> getMyDrafts(Long userId) {
        return settingRepository.findByAuthorIdAndStatusAndDeletedFalse(userId, Setting.SettingStatus.draft);
    }

    @Transactional
    public void deleteSetting(Long settingId, Long userId) {
        Setting setting = settingRepository.findByIdNotDeleted(settingId)
                .orElseThrow(() -> new RuntimeException("设定不存在"));
        if (!setting.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权删除此设定");
        }
        settingRepository.softDeleteById(settingId);
    }


    private boolean isCircularReference(Setting setting, Long targetId) {
        Setting current = setting;
        while (current != null) {
            if (current.getId().equals(targetId)) {
                return true;
            }
            current = current.getParentSetting();
        }
        return false;
    }

    // 查询子设定（分页，仅已发布）
    @Transactional(readOnly = true)
    public Page<Setting> getChildrenByParentId(Long parentId, Pageable pageable) {
        return settingRepository.findByParentSettingIdAndStatusAndDeletedFalse(parentId, Setting.SettingStatus.published, pageable);
    }

    @Transactional(readOnly = true)
    public Setting getSettingEntityById(Long id) {
        Setting setting = settingRepository.findById(id).orElse(null);
        if (setting != null) {
            Hibernate.initialize(setting.getAuthor());
            Hibernate.initialize(setting.getTags());
            Hibernate.initialize(setting.getChildren());
            setting.getChildren().forEach(c -> Hibernate.initialize(c.getAuthor()));
        }
        return setting;
    }


    public Page<Setting> searchSettings(String keyword, String category, Set<String> tags, Pageable pageable) {
        Specification<Setting> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 只查已发布且未删除
            predicates.add(cb.equal(root.get("status"), Setting.SettingStatus.published));
            predicates.add(cb.isFalse(root.get("deleted")));

            if (StringUtils.hasText(keyword)) {
                Predicate titleLike = cb.like(root.get("title"), "%" + keyword + "%");
                Predicate contentLike = cb.like(root.get("content"), "%" + keyword + "%");
                predicates.add(cb.or(titleLike, contentLike));
            }
            if (StringUtils.hasText(category)) {
                try {
                    Setting.SettingCategory cat = Setting.SettingCategory.valueOf(category);
                    predicates.add(cb.equal(root.get("category"), cat));
                } catch (IllegalArgumentException ignored) {}
            }
            if (tags != null && !tags.isEmpty()) {
                for (String tagName : tags) {
                    Join<Setting, Tag> tagJoin = root.join("tags");
                    predicates.add(cb.equal(tagJoin.get("name"), tagName));
                }
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return settingRepository.findAll(spec, pageable);
    }


    public Page<Setting> getPublishedSettingsByUserId(Long userId, Pageable pageable) {
        return settingRepository.findByAuthorIdAndStatusAndDeletedFalse(userId, Setting.SettingStatus.published, pageable);
    }

    /**
     * 注册浏览，返回 true 表示本次计入有效浏览，false 表示重复不计
     */
    public boolean registerView(Long settingId, Long userId, String ip, String userAgent) {
        // 生成去重 key：view:setting:{settingId}:{date}:{ip}:{userAgentHash}
        String date = LocalDate.now().toString();
        String uaHash = DigestUtils.md5DigestAsHex(userAgent.getBytes()); // 简化 User-Agent 哈希
        String key = String.format("view:setting:%d:%s:%s:%s", settingId, date, ip, uaHash);
        // 如果已存在则直接返回 false
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofDays(1));
        if (Boolean.TRUE.equals(success)) {
            // 增加文章浏览量
            settingRepository.incrementViewCount(settingId);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public SettingResponse getSettingDetail(Long id) {
        Setting setting = settingRepository.findByIdWithAll(id)
                .orElseThrow(() -> new RuntimeException("设定不存在"));

        if (setting.getChildren() != null) {
            setting.getChildren().size();
            for (Setting child : setting.getChildren()) {
                Hibernate.initialize(child.getAuthor());
            }
        }
        return SettingResponse.fromSetting(setting);
    }

}