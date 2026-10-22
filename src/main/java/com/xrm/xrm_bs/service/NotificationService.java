package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.NotificationGroupResponse;
import com.xrm.xrm_bs.dto.NotificationResponse;
import com.xrm.xrm_bs.entity.*;
import com.xrm.xrm_bs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final CommentRepository commentRepository;


    private void cleanDuplicateBeforeCreate(Long userId, String type, Long targetId, Long sourceUserId) {
        notificationRepository.deleteDuplicateNotifications(userId, type, targetId, sourceUserId);
    }

    public void createNotification(Long userId, String type, Long sourceUserId,
                                   Long targetId, String targetType, String content) {
        createNotification(userId, type, sourceUserId, targetId, targetType, content, null);
    }

    public void createNotification(Long userId, String type, Long sourceUserId,
                                   Long targetId, String targetType, String content, Long commentId) {
        // 不给自己发通知
        if (userId.equals(sourceUserId)) return;


        if (type.startsWith("like_") || type.startsWith("favorite_") || type.equals("follow")) {
            cleanDuplicateBeforeCreate(userId, type, targetId, sourceUserId);
        }
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setSourceUserId(sourceUserId);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCommentId(commentId);

        notificationRepository.save(notification);
    }

    // 获取未读数量
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // 标记为已读
    @Transactional
    public void markAsRead(Long userId, List<Long> ids) {
        notificationRepository.markAsRead(userId, ids);
    }

    // 全部标记已读
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse resp = new NotificationResponse();
        resp.setId(n.getId());
        resp.setType(n.getType());
        resp.setSourceUserId(n.getSourceUserId());
        resp.setTargetId(n.getTargetId());
        resp.setTargetType(n.getTargetType());
        resp.setContent(n.getContent());
        resp.setIsRead(n.getIsRead());
        resp.setCreatedAt(n.getCreatedAt());
        resp.setCommentId(n.getCommentId());
        // 获取触发者用户名
        userRepository.findById(n.getSourceUserId()).ifPresent(u -> resp.setSourceUsername(u.getUsername()));
        return resp;
    }



    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        notificationRepository.deleteByIdAndUserId(notificationId, userId);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }


    private String buildGroupKey(Notification n) {
        String type = n.getType();
        if (type.equals("follow")) {
            return type + "_" + n.getId();
        }
        String targetType = n.getTargetType();
        Long targetId = n.getTargetId();



        if (type.startsWith("like_") || type.startsWith("favorite_")) {
            return type + "_" + targetType + "_" + targetId;
        }
        // 一级评论通知：按文章/设定聚合
        else if (type.equals("comment_article") || type.equals("comment_setting")) {
            return "comment_" + targetType + "_" + targetId;
        }
        // 回复评论通知：按父评论聚合
        else if (type.equals("reply_comment")) {
            Long parentId = commentRepository.findById(targetId)
                    .map(Comment::getParentId).orElse(targetId);
            return "reply_" + targetType + "_" + parentId;
        }
        // 其他：不聚合，使用通知ID
        return type + "_" + n.getId();
    }


    /**
     * 构建分组响应
     */
    private NotificationGroupResponse buildGroupResponse(String groupKey, List<Notification> list) {


        Notification first = list.get(0);
        NotificationGroupResponse group = new NotificationGroupResponse();
        group.setGroupKey(groupKey);
        group.setType(first.getType());
        group.setTargetId(first.getTargetId());
        group.setTargetType(first.getTargetType());
        group.setCommentId(first.getCommentId());
        group.setTotalCount(list.size());
        group.setUnreadCount((int) list.stream().filter(n -> !n.getIsRead()).count());
        group.setLatestTime(first.getCreatedAt());

        // 生成标题和预览内容
        buildGroupTitleAndPreview(group, list);

        return group;
    }


    private void buildGroupTitleAndPreview(NotificationGroupResponse group, List<Notification> list) {
        if ("follow".equals(group.getType())) {
            group.setTitle("新关注");
            String preview = list.get(0).getContent();
            if (preview == null || preview.isEmpty()) {
                User sourceUser = userRepository.findById(list.get(0).getSourceUserId()).orElse(null);
                preview = (sourceUser != null ? sourceUser.getUsername() : "有人") + " 关注了你";
            }
            group.setPreviewContent(preview);
            return; // 处理完直接返回，不再走后面的逻辑
        }

        // 2. 只有非关注类型的 groupKey 需要 split 且长度通常 >= 3
        String groupKey = group.getGroupKey();
        String[] parts = groupKey.split("_");
        if (parts.length < 3) return;

        String prefix = parts[0];
        String targetType = parts[1];
        Long targetId = Long.parseLong(parts[parts.length - 1]);

        String targetTitle = getTargetTitle(targetType, targetId);
        List<String> usernames = list.stream()
                .map(n -> userRepository.findById(n.getSourceUserId()).map(User::getUsername).orElse("未知用户"))
                .distinct()
                .collect(Collectors.toList());

        if (prefix.equals("like")) {
            group.setTitle(getTypeName(targetType) + "《" + targetTitle + "》");
            group.setPreviewContent(buildLikePreview(usernames, list.size()) + " 赞了");
        } else if (prefix.equals("favorite")) {
            group.setTitle(getTypeName(targetType) + "《" + targetTitle + "》");
            group.setPreviewContent(buildLikePreview(usernames, list.size()) + " 收藏了");
        } else if (prefix.equals("comment")) {
            group.setTitle(getTypeName(targetType) + "《" + targetTitle + "》");
            group.setPreviewContent(usernames.size() + " 人发表了 " + list.size() + " 条评论");
        } else if (prefix.equals("reply")) {
            String commentPreview = getCommentPreview(targetId);
            group.setTitle("评论：“" + commentPreview + "”");
            group.setPreviewContent(usernames.size() + " 人回复了 " + list.size() + " 条");
        } else {
            group.setTitle("通知");
            group.setPreviewContent(list.get(0).getContent());
        }
    }

    private String getTypeName(String targetType) {
        switch (targetType) {
            case "article": return "文章";
            case "setting": return "设定";
            case "comment": return "评论";
            default: return "内容";
        }
    }

    private String buildLikePreview(List<String> usernames, int totalCount) {
        if (usernames.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(usernames.get(0));
        if (usernames.size() > 1) {
            sb.append("、").append(usernames.get(1));
        }
        if (totalCount > 2) {
            sb.append(" 等 ").append(totalCount).append(" 人");
        }
        return sb.toString();
    }

    private String getTargetTitle(String targetType, Long targetId) {
        if ("article".equals(targetType)) {
            return articleRepository.findById(targetId).map(Article::getTitle).orElse("已删除");
        } else if ("setting".equals(targetType)) {
            return settingRepository.findById(targetId).map(Setting::getTitle).orElse("已删除");
        } else if ("comment".equals(targetType)) {
            return getCommentPreview(targetId);
        }
        return "";
    }

    private String getCommentPreview(Long commentId) {
        return commentRepository.findById(commentId)
                .map(c -> c.getContent().length() > 30 ? c.getContent().substring(0, 30) + "…" : c.getContent())
                .orElse("评论已删除");
    }

    /**
     * 获取分组内详情（用于展开）
     */

    @Transactional(readOnly = true)
    public List<NotificationResponse> getGroupItems(Long userId, String groupKey) {
        String[] parts = groupKey.split("_");
        
        // 处理关注通知（follow_123 格式）
        if (groupKey.startsWith("follow_")) {
            if (parts.length < 2) {
                return Collections.emptyList();
            }
            Long id = Long.parseLong(parts[1]);
            return notificationRepository.findById(id)
                    .map(n -> List.of(toResponse(n)))
                    .orElse(Collections.emptyList());
        }
        
        if (parts.length < 3) {
            return Collections.emptyList();
        }


        if (groupKey.startsWith("like_") || groupKey.startsWith("favorite_")) {
            String targetIdStr = parts[parts.length - 1];
            Long targetId = Long.parseLong(targetIdStr);
            String type = parts[0] + "_" + parts[1];
            String targetType = parts[1];

            List<Notification> items = notificationRepository
                    .findByUserIdAndTypeAndTargetTypeAndTargetIdOrderByCreatedAtDesc(
                            userId, type, targetType, targetId);
            return items.stream().map(this::toResponse).collect(Collectors.toList());
        }

        String prefix = parts[0];
        String targetType = parts[1];

        if (prefix.equals("comment")) {
            Long targetId = Long.parseLong(parts[2]);
            List<Notification> items = notificationRepository
                    .findByUserIdAndTargetTypeAndTargetIdAndTypeInOrderByCreatedAtDesc(
                            userId, targetType, targetId,
                            List.of("comment_article", "comment_setting", "reply_comment"));
            return items.stream().map(this::toResponse).collect(Collectors.toList());
        } else if (prefix.equals("reply")) {
            Long parentId = Long.parseLong(parts[2]);
            List<Long> childCommentIds = commentRepository.findByParentIdAndStatus(parentId, 1).stream()
                    .map(Comment::getId).collect(Collectors.toList());
            childCommentIds.add(parentId);
            List<Notification> items = notificationRepository
                    .findByUserIdAndTargetIdInAndTypeOrderByCreatedAtDesc(userId, childCommentIds, "reply_comment");
            return items.stream().map(this::toResponse).collect(Collectors.toList());
        } else {
            // 其他单条
            Long id = Long.parseLong(parts[parts.length - 1]);
            return notificationRepository.findById(id)
                    .map(n -> List.of(toResponse(n)))
                    .orElse(Collections.emptyList());
        }
    }


    @Transactional(readOnly = true)
    public Page<NotificationGroupResponse> getGroupedNotifications(Long userId, String filter, Pageable pageable) {
        // 1. 一次性查询较多原始通知（例如500条），用于分组聚合，避免全表扫描
        Pageable largePageable = PageRequest.of(0, 500, Sort.by("createdAt").descending());
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, largePageable);
        List<Notification> notifications = notificationPage.getContent();

        // 2. 根据 filter 过滤
        Stream<Notification> stream = notifications.stream();
        if ("like_favorite".equals(filter)) {
            stream = stream.filter(n -> n.getType().startsWith("like_") || n.getType().startsWith("favorite_"));
        } else if ("reply".equals(filter)) {
            stream = stream.filter(n -> n.getType().startsWith("comment_") || n.getType().startsWith("reply_"));
        } else if ("follow".equals(filter)) {
//            stream = stream.filter(n -> n.getType().equals("follow"));
            stream = stream.filter(n -> n.getType().startsWith("follow"));
        }
        List<Notification> filtered = stream.collect(Collectors.toList());

        // 3. 分组（保持插入顺序）
        Map<String, List<Notification>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(this::buildGroupKey, LinkedHashMap::new, Collectors.toList()));

        List<NotificationGroupResponse> allGroups = grouped.entrySet().stream()
                .map(entry -> buildGroupResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // 4. 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allGroups.size());
        List<NotificationGroupResponse> pageContent = (start < end)
                ? allGroups.subList(start, end)
                : Collections.emptyList();

        // 总数使用分组后的总条数
        return new PageImpl<>(pageContent, pageable, allGroups.size());
    }
}