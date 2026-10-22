// TagRepository.java
package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {

    // 原有方法
    Optional<Tag> findByName(String name);
    boolean existsByName(String name);

    //热门标签（按使用次数）
    List<Tag> findTop10ByOrderByUsageCountDesc();

    //更新标签使用次数
    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount + 1, t.lastUsedAt = CURRENT_TIMESTAMP WHERE t.id = :tagId")
    void incrementUsageCount(@Param("tagId") Long tagId);

    //分页查询
    Page<Tag> findAll(Pageable pageable);

    List<Tag> findTop10ByClassificationOrderByUsageCountDesc(Tag.Classification classification);

    // 搜索时增加分类过滤
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND t.classification = :classification ORDER BY t.usageCount DESC")
    List<Tag> searchByKeywordAndClassification(@Param("keyword") String keyword, @Param("classification") Tag.Classification classification);

    Optional<Tag> findByNameAndType(String name, Tag.TagType type);

    boolean existsByNameAndClassification(String name, Tag.Classification classification);

    @Query("SELECT t FROM Tag t WHERE t.classification = :classification ORDER BY t.usageCount DESC")
    List<Tag> findByClassificationOrderByUsageCountDesc(@Param("classification") Tag.Classification classification, Pageable pageable);

    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.usageCount DESC")
    List<Tag> searchByKeyword(@Param("keyword") String keyword);
}