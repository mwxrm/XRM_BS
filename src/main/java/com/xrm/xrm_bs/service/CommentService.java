package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.CommentRequest;
import com.xrm.xrm_bs.dto.CommentResponse;
import com.xrm.xrm_bs.entity.*;
import com.xrm.xrm_bs.repository.*;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository likeRepository;
    private final NotificationService notificationService;
    private final UserBehaviorLogService behaviorLogService;
    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final TagRepository tagRepository;

    // 创建评论
    @Transactional
    public CommentResponse createComment(String targetType, Long targetId, CommentRequest request, Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setUserId(userId);
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setParentId(request.getParentId());
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment = commentRepository.save(comment);

        // 发送通知
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long parentId = request.getParentId();  // 获取父评论ID
        if (parentId == null) {
            // 一级评论：给内容作者发通知
            Long contentAuthorId = null;
            String title = "";
            if ("article".equals(targetType)) {
                Article article = articleRepository.findById(targetId).orElse(null);
                if (article != null) {
                    contentAuthorId = article.getAuthor().getId();
                    title = article.getTitle();
                }
            } else if ("setting".equals(targetType)) {
                Setting setting = settingRepository.findById(targetId).orElse(null);
                if (setting != null) {
                    contentAuthorId = setting.getAuthor().getId();
                    title = setting.getTitle();
                }
            } else if ("tag".equals(targetType)) {
                Tag tag = tagRepository.findById(targetId).orElse(null);
                if (tag != null && tag.getCreatedBy() != null) {
                    contentAuthorId = tag.getCreatedBy();
                    title = tag.getName();
                }
            }
            if (contentAuthorId != null && !contentAuthorId.equals(currentUserId)) {
                String content = "评论了你的" + getContentTypeName(targetType) + "《" + title + "》";
                notificationService.createNotification(
                        contentAuthorId,
                        "comment_" + targetType,
                        currentUserId,
                        targetId,
                        targetType,
                        content,
                        comment.getId()
                );
            }
        } else {
            // 回复评论：给父评论作者发通知
            Comment parentComment = commentRepository.findById(parentId).orElse(null);
            if (parentComment != null && !parentComment.getUserId().equals(currentUserId)) {
                String content = "回复了你的评论";
                notificationService.createNotification(parentComment.getUserId(),
                        "reply_comment",
                        currentUserId,
                        parentId,
                        "comment",
                        content,
                        comment.getId()
                );
            }
        }
        if ("article".equals(targetType) || "setting".equals(targetType)) {
            behaviorLogService.log(userId, targetId, targetType, "comment");
        }
        return toResponse(comment, user, false);
    }

    private String getContentTypeName(String targetType) {
        switch (targetType) {
            case "article": return "文章";
            case "setting": return "设定";
            case "tag": return "标签";
            default: return "内容";
        }
    }


    // 获取评论列表（带二级回复）
    public Page<CommentResponse> getComments(String targetType, Long targetId, Pageable pageable, Long currentUserId) {
        // 1. 查询一级评论（parentId IS NULL 且 status = 1）
        Page<Comment> page = commentRepository.findByTargetTypeAndTargetIdAndParentIdIsNullAndStatus(
                targetType, targetId, 1, pageable);
        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. 收集一级评论的ID
        List<Long> topLevelIds = page.getContent().stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        // 3. 查询所有二级评论（status = 1）
        List<Comment> allReplies = commentRepository.findByParentIdInAndStatus(topLevelIds, 1);
        // 按父ID分组
        Map<Long, List<Comment>> repliesMap = allReplies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 4. 收集所有涉及的用户ID（一级评论作者 + 二级评论作者）
        Set<Long> userIds = new HashSet<>();
        page.getContent().forEach(c -> userIds.add(c.getUserId()));
        allReplies.forEach(r -> userIds.add(r.getUserId()));
        // 批量查询用户信息
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 5. 收集所有评论ID（包括一级和二级）
        List<Long> allCommentIds = new ArrayList<>(topLevelIds);
        allCommentIds.addAll(allReplies.stream().map(Comment::getId).collect(Collectors.toList()));

        // 6. 查询当前用户对哪些评论点过赞
        final Map<Long, Boolean> likedMap = new HashMap<>();  // 声明为 final
        if (currentUserId != null && !allCommentIds.isEmpty()) {
            List<CommentLike> likes = likeRepository.findByCommentIdInAndUserId(allCommentIds, currentUserId);
            likedMap.putAll(likes.stream().collect(Collectors.toMap(CommentLike::getCommentId, like -> true)));
        }

        // 7. 转换一级评论（无需修改，lambda 内部可直接使用 likedMap，因为它是 final 的）
        List<CommentResponse> responses = page.getContent().stream()
                .map(comment -> {
                    User author = userMap.get(comment.getUserId());
                    List<CommentResponse> replyResponses = repliesMap.getOrDefault(comment.getId(), Collections.emptyList()).stream()
                            .map(reply -> {
                                User replyAuthor = userMap.get(reply.getUserId());
                                return toResponse(reply, replyAuthor, likedMap.getOrDefault(reply.getId(), false));
                            })
                            .collect(Collectors.toList());
                    return toResponse(comment, author, likedMap.getOrDefault(comment.getId(), false), replyResponses);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }



    // 删除评论
    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (!comment.getUserId().equals(userId) && !isAdmin) {
            throw new RuntimeException("无权删除");
        }
        comment.setStatus(0); // 软删除
        commentRepository.save(comment);
    }

    // 检查是否已点赞（暂未实现点赞记录表，后续可扩展）
    // 基础转换（无回复列表）
    // 基础版本：不带回复列表
    private CommentResponse toResponse(Comment comment, User author, boolean likedByCurrentUser) {
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setContent(comment.getContent());
        resp.setUserId(comment.getUserId());
        resp.setAuthorUsername(author.getUsername());
        resp.setAuthorAvatar("https://ui-avatars.com/api/?name=" + author.getUsername() + "&background=409eff&color=fff");
        resp.setParentId(comment.getParentId());
        resp.setLikeCount(comment.getLikeCount());
        resp.setLikedByCurrentUser(likedByCurrentUser);
        resp.setCreatedAt(comment.getCreatedAt());
        resp.setReplies(new ArrayList<>());
        resp.setCommentId(comment.getId());
        return resp;
    }


    @Transactional
    public void likeComment(Long commentId, Long userId) {
        if (likeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new RuntimeException("已经点过赞");
        }
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        likeRepository.save(like);
        commentRepository.incrementLikeCount(commentId);

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null && !comment.getUserId().equals(userId)) {
            String content = "赞了你的评论";
//            notificationService.createNotification(comment.getUserId(), "like_comment", userId, commentId, "comment", content);
            notificationService.createNotification(comment.getUserId(), "like_comment", userId, commentId, "comment", content, commentId);
        }
    }

    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        likeRepository.deleteByCommentIdAndUserId(commentId, userId);
        commentRepository.decrementLikeCount(commentId);
    }

    // 重载版本：带回复列表
    private CommentResponse toResponse(Comment comment, User author, boolean likedByCurrentUser, List<CommentResponse> replies) {
        CommentResponse resp = toResponse(comment, author, likedByCurrentUser);
        resp.setReplies(replies);
        return resp;
    }

    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        User author = userRepository.findById(comment.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return toResponse(comment, author, false);
    }


    public Map<String, Object> getCommentContext(Long commentId) {
        Comment target = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        Map<String, Object> result = new HashMap<>();

        // 1. 目标评论详情（含作者信息）
        User targetAuthor = userRepository.findById(target.getUserId()).orElse(null);
        CommentResponse targetResp = toResponse(target, targetAuthor, false);
        result.put("targetComment", targetResp);

        // 2. 如果是一级评论，直接返回其本身作为置顶项；如果是二级评论，则返回其父评论
        CommentResponse topComment;
        if (target.getParentId() == null) {
            topComment = targetResp;
        } else {
            Comment parent = commentRepository.findById(target.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));
            User parentAuthor = userRepository.findById(parent.getUserId()).orElse(null);
            topComment = toResponse(parent, parentAuthor, false);
        }
        result.put("topComment", topComment);

        // 获取置顶评论的所有二级回复（status = 1）
        if (topComment.getId() != null) {
            List<Comment> replies = commentRepository.findByParentIdInAndStatus(
                    Collections.singletonList(topComment.getId()), 1);

            // 转换二级回复为响应对象
            List<CommentResponse> replyResps = replies.stream()
                    .map(r -> toResponse(r, userRepository.findById(r.getUserId()).orElse(null), false))
                    .collect(Collectors.toList());

            // ========== 关键新增：将目标二级评论置顶 ==========
            // 如果目标评论是二级评论（即 parentId != null），且其父评论就是 topComment
            if (target.getParentId() != null && target.getParentId().equals(topComment.getId())) {
                // 查找目标评论在列表中的位置
                Optional<CommentResponse> targetReplyOpt = replyResps.stream()
                        .filter(r -> r.getId().equals(commentId))
                        .findFirst();

                if (targetReplyOpt.isPresent()) {
                    CommentResponse targetReply = targetReplyOpt.get();
                    // 从原列表中移除
                    replyResps.remove(targetReply);
                    // 插入到列表首位
                    replyResps.add(0, targetReply);
                }
            }

            topComment.setReplies(replyResps);
        }

        result.put("topComment", topComment);
        result.put("targetComment", targetResp);

        // 4. 可选：返回总评论数，用于显示
        long total = commentRepository.countByTargetTypeAndTargetIdAndParentIdIsNullAndStatus(
                target.getTargetType(), target.getTargetId(), 1);
        result.put("total", total);

        return result;
    }



}