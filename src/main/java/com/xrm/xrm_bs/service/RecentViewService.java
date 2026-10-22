package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.RecentViewResponse;
import com.xrm.xrm_bs.entity.Article;
import com.xrm.xrm_bs.entity.RecentView;
import com.xrm.xrm_bs.entity.Setting;
import com.xrm.xrm_bs.repository.ArticleRepository;
import com.xrm.xrm_bs.repository.RecentViewRepository;
import com.xrm.xrm_bs.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RecentViewService {

    private final RecentViewRepository recentViewRepository;
    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;

    // 新增：异步 + 独立事务，避免死锁
    @Async("emailExecutor")
    public void addRecentViewSafe(Long userId, Long itemId, String itemType) {
        try {
            doAddRecentView(userId, itemId, itemType);
        } catch (Exception e) {
            // 并发冲突时静默忽略，不抛出异常
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doAddRecentView(Long userId, Long itemId, String itemType) {
        recentViewRepository.deleteByUserIdAndItemIdAndItemType(userId, itemId, itemType);
        RecentView recent = new RecentView();
        recent.setUserId(userId);
        recent.setItemId(itemId);
        recent.setItemType(itemType);
        recentViewRepository.save(recent);
    }

    public Page<RecentViewResponse> getUserRecentViews(Long userId, Pageable pageable) {
        Page<RecentView> viewPage = recentViewRepository.findByUserIdOrderByViewedAtDesc(userId, pageable);
        List<RecentViewResponse> responses = viewPage.getContent().stream()
                .map(view -> {
                    if ("article".equals(view.getItemType())) {
                        Article article = articleRepository.findById(view.getItemId()).orElse(null);
                        RecentViewResponse resp = new RecentViewResponse();
                        resp.setId(view.getId());
                        resp.setItemId(view.getItemId());
                        resp.setItemType("article");
                        resp.setViewedAt(view.getViewedAt());
                        if (article == null) {
                            resp.setDeleted(true);
                            resp.setTitle("已删除的文章");
                            resp.setAuthorUsername("未知");
                            resp.setSummary("");
                        } else {
                            resp.setDeleted(article.getDeleted());
                            resp.setTitle(article.getTitle());
                            resp.setAuthorUsername(article.getAuthor().getUsername());
                            resp.setSummary(article.getContent().length() > 100 ?
                                    article.getContent().substring(0, 100) + "..." : article.getContent());
                        }
                        return resp;
                    }
                    else if ("setting".equals(view.getItemType())) {
                        Setting setting = settingRepository.findById(view.getItemId()).orElse(null);
                        RecentViewResponse resp = new RecentViewResponse();
                        resp.setId(view.getId());
                        resp.setItemId(view.getItemId());
                        resp.setItemType("setting");
                        resp.setViewedAt(view.getViewedAt());
                        if (setting == null) {
                            resp.setDeleted(true);
                            resp.setTitle("已删除的设定");
                            resp.setAuthorUsername("未知");
                            resp.setSummary("");
                        } else {
                            resp.setDeleted(setting.getDeleted());   // 关键：设置 deleted 字段
                            resp.setTitle(setting.getTitle());
                            resp.setAuthorUsername(setting.getAuthor().getUsername());
                            resp.setSummary(setting.getContent().length() > 100 ?
                                    setting.getContent().substring(0, 100) + "..." : setting.getContent());
                        }
                        return resp;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, viewPage.getTotalElements());
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        recentViewRepository.deleteByUserId(userId);
    }



}