package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.*;
import com.xrm.xrm_bs.entity.Article;
import com.xrm.xrm_bs.entity.Setting;
import com.xrm.xrm_bs.service.ArticleService;
import com.xrm.xrm_bs.service.RecentViewService;
import com.xrm.xrm_bs.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;




@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "*")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private RecentViewService recentViewService;

    // 创建文章（保存为草稿）
    @PostMapping
    public ResponseEntity<?> createArticle(@RequestBody CreateArticleRequest request) {
        try {
            System.out.println("=== 创建文章请求开始 ===");
            System.out.println("请求数据: " + request);

            Long authorId = SecurityUtil.getCurrentUserId();
            System.out.println("当前用户ID: " + authorId);

            Article article = articleService.createArticle(request, authorId);
            System.out.println("文章创建成功，ID: " + article.getId());
            System.out.println("文章状态: " + article.getStatus());
            System.out.println("文章状态名称: " + article.getStatus().name());

            ArticleResponse response = ArticleResponse.fromArticle(article);
            System.out.println("响应对象状态: " + response.getStatus());
            System.out.println("=== 创建文章请求结束 ===");

//            return ResponseEntity.ok(response);
            return ResponseEntity.ok(ArticleResponse.fromArticle(article));
        } catch (Exception e) {
            System.err.println("创建文章时出错: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 获取当前用户的草稿
    @GetMapping("/drafts")
    public ResponseEntity<?> getMyDrafts() {
        try {
            Long authorId = SecurityUtil.getCurrentUserId();
            List<Article> drafts = articleService.getMyDrafts(authorId);
            List<SimpleArticleResponse> responses = drafts.stream()
                    .map(SimpleArticleResponse::fromArticle)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 获取当前用户的所有文章（草稿和已发布）
    @GetMapping("/my")
    public ResponseEntity<?> getMyArticles() {
        try {
            Long authorId = SecurityUtil.getCurrentUserId();
            List<Article> articles = articleService.getMyArticles(authorId);
            List<SimpleArticleResponse> responses = articles.stream()
                    .map(SimpleArticleResponse::fromArticle)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/published")
    public ResponseEntity<Page<SimpleArticleResponse>> getPublishedArticles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Article> articles = articleService.getPublishedArticles(pageable);
        return ResponseEntity.ok(articles.map(SimpleArticleResponse::fromArticle));
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getArticle(@PathVariable Long id) {
        try {
            ArticleResponse articleResp = articleService.getArticleDetail(id);

            if (articleResp.getDeleted()) {
                return ResponseEntity.ok(articleResp);
            }

            Article article = articleService.getArticleEntityById(id); // 这里仍可调用原来的获取，但不用于响应
            if (article.getStatus() == Article.ArticleStatus.draft) {
                Long currentUserId = SecurityUtil.getCurrentUserId();
                if (!article.getAuthor().getId().equals(currentUserId)) {
                    return ResponseEntity.status(403).body("无权查看此文章");
                }
            }

            if (article == null) {
                return ResponseEntity.notFound().build();
            }

            // 如果文章已被软删除，直接返回响应（不增加浏览量、不记录最近浏览、不检查草稿权限）
            if (article.getDeleted()) {
                return ResponseEntity.ok(ArticleResponse.fromArticle(article));
            }

            // 检查权限：如果是草稿，只有作者可以查看
            if (article.getStatus() == Article.ArticleStatus.draft) {
                Long currentUserId = SecurityUtil.getCurrentUserId();
                if (!article.getAuthor().getId().equals(currentUserId)) {
                    return ResponseEntity.status(403).body("无权查看此文章");
                }
            }

            // 记录最近浏览（仅正常文章）
            try {
                Long currentUserId = SecurityUtil.getCurrentUserId();
//                recentViewService.addRecentView(currentUserId, id, "article");
                recentViewService.addRecentViewSafe(currentUserId, id, "article");
            } catch (Exception e) {
                // 忽略
            }

            // 增加浏览量、记录浏览等
            return ResponseEntity.ok(articleResp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // 更新文章
    @PutMapping("/{id}")
    public ResponseEntity<?> updateArticle(@PathVariable Long id, @RequestBody UpdateArticleRequest request) {
        try {
            Long authorId = SecurityUtil.getCurrentUserId();
            Article article = articleService.updateArticle(id, request, authorId);
            return ResponseEntity.ok(ArticleResponse.fromArticle(article));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 发布文章
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishArticle(@PathVariable Long id) {
        try {
            Long authorId = SecurityUtil.getCurrentUserId();
            Article article = articleService.publishArticle(id, authorId);
            return ResponseEntity.ok(ArticleResponse.fromArticle(article));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 删除文章
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable Long id) {
        try {
            Long authorId = SecurityUtil.getCurrentUserId();
            articleService.deleteArticle(id, authorId);
            return ResponseEntity.ok("文章删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 根据标签搜索文章
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<?> getArticlesByTag(@PathVariable String tagName) {
        try {
            List<Article> articles = articleService.getArticlesByTag(tagName);
            List<SimpleArticleResponse> responses = articles.stream()
                    .map(SimpleArticleResponse::fromArticle)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ArticleController.java
    @GetMapping("/{id}/children")
    public ResponseEntity<?> getChildren(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Article> childrenPage = articleService.getChildrenByParentId(id, pageable);
        Page<ArticleResponse.SimpleArticleInfo> response = childrenPage.map(ArticleResponse.SimpleArticleInfo::from);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<SimpleArticleResponse>> searchArticles(@RequestBody ArticleSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy()));
        Page<Article> articles = articleService.searchArticles(request.getKeyword(), request.getTags(), pageable);
        return ResponseEntity.ok(articles.map(SimpleArticleResponse::fromArticle));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<SimpleArticleResponse>> getUserArticles(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Article> articles = articleService.getPublishedArticlesByUserId(userId, pageable);
        return ResponseEntity.ok(articles.map(SimpleArticleResponse::fromArticle));
    }


    @PostMapping("/{id}/view")
    public ResponseEntity<?> reportArticleView(@PathVariable Long id,
                                               @RequestBody(required = false) Map<String, Object> payload,
                                               HttpServletRequest request) {
        try {
            // 获取客户端信息
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            Long userId = null;
            try {
                userId = SecurityUtil.getCurrentUserId();
            } catch (Exception e) {
                // 未登录用户，userId 为 null
            }
            boolean counted = articleService.registerView(id, userId, ip, userAgent);
            if (counted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(409).body("今日已计过浏览");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 获取文章关联的设定
    @GetMapping("/{id}/settings")
    public ResponseEntity<List<SettingResponse>> getArticleSettings(@PathVariable Long id) {
        return ResponseEntity.ok(
                articleService.getArticleSettings(id).stream()
                        .map(SettingResponse::fromSetting)
                        .collect(Collectors.toList())
        );
    }

    // 更新关联设定（替换整个列表）
    @PutMapping("/{id}/settings")
    public ResponseEntity<?> updateArticleSettings(@PathVariable Long id,
                                                   @RequestBody List<Long> settingIds) {
        Long userId = SecurityUtil.getCurrentUserId();
        Article article = articleService.getArticleEntityById(id);
        if (!article.getAuthor().getId().equals(userId))
            return ResponseEntity.status(403).body("无权修改");
        articleService.updateArticleSettings(id, settingIds);
        return ResponseEntity.ok("更新成功");
    }

}