package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.ArticleSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArticleSettingRepository extends JpaRepository<ArticleSetting, Long> {
    List<ArticleSetting> findByArticleId(Long articleId);
    boolean existsByArticleIdAndSettingId(Long articleId, Long settingId);

    @Query("SELECT COUNT(a) FROM ArticleSetting a WHERE a.settingId = :settingId")
    long countBySettingId(@Param("settingId") Long settingId);

}