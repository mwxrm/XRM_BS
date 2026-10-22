package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class NotificationGroupResponse {
    private String groupKey;          // 分组唯一标识，如 "like_article_article_123"
    private String type;              // like_article, like_setting, like_comment, reply_comment...
    private String title;             // 聚合标题，如“文章《XXX》”
    private String previewContent;    // 预览内容，如“张三、李四 等5人 赞了”
    private Long targetId;
    private String targetType;
    private Long commentId;           // 若是评论相关，携带评论ID（用于跳转）
    private Integer totalCount;       // 该分组内通知总数
    private Integer unreadCount;      // 未读数量
    private LocalDateTime latestTime; // 最新一条通知的时间
    private List<NotificationResponse> items; // 分组内详细通知（点击展开后加载）
}