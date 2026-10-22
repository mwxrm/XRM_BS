package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.Article;
import jakarta.transaction.Transactional;
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
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    // 分页查询已发布的文章
    Page<Article> findByStatus(Article.ArticleStatus status, Pageable pageable);

    // 加载文章详情（包含作者、标签、父文章、子文章，且都未删除）
    @Query("SELECT a FROM Article a " +
            "LEFT JOIN FETCH a.author " +
            "LEFT JOIN FETCH a.tags " +
            "LEFT JOIN FETCH a.parentArticle " +
            "LEFT JOIN FETCH a.children " +
            "WHERE a.id = :id")
    Optional<Article> findByIdWithAll(@Param("id") Long id);


    Page<Article> findByParentArticleIdAndStatus(Long parentId, Article.ArticleStatus status, Pageable pageable);

    // 根据标签查找已发布的文章（未删除）
    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t.name = :tagName AND a.status = 'published' AND a.deleted = false")
    List<Article> findByTagName(@Param("tagName") String tagName);


    // 查找已发布的文章（未删除）
    List<Article> findByStatusAndDeletedFalseOrderByCreatedAtDesc(Article.ArticleStatus status);

    // 查找作者的所有文章（未删除）
    List<Article> findByAuthorIdAndDeletedFalse(Long authorId);

    // 查找作者的草稿（未删除）
    List<Article> findByAuthorIdAndStatusAndDeletedFalse(Long authorId, Article.ArticleStatus status);

    Page<Article> findByAuthorIdAndStatusAndDeletedFalse(Long authorId, Article.ArticleStatus status, Pageable pageable);

    // 查询所有已发布未删除的文章（用于推荐遍历）
    List<Article> findAllByStatusAndDeletedFalse(Article.ArticleStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(a.likeCount), 0) FROM Article a WHERE a.author.id = :authorId AND a.deleted = false")
    long sumLikeCountByAuthorId(@Param("authorId") Long authorId);


    @Query("SELECT COUNT(a) FROM Article a WHERE a.parentArticle.id = :parentId AND a.status = :status AND a.deleted = false")
    long countByParentArticleIdAndStatus(@Param("parentId") Long parentId, @Param("status") Article.ArticleStatus status);


}