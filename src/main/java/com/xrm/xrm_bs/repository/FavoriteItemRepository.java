package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.FavoriteItem;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {

    // 基础分页查询（按文件夹）
    Page<FavoriteItem> findByFolderId(Long folderId, Pageable pageable);

    // 按文件夹和类型分页查询
    Page<FavoriteItem> findByFolderIdAndItemType(Long folderId, String itemType, Pageable pageable);

    // 检查是否存在
    boolean existsByFolderIdAndItemTypeAndItemId(Long folderId, String itemType, Long itemId);

    // 删除
    @Modifying
    @Transactional
    void deleteByFolderIdAndItemTypeAndItemId(Long folderId, String itemType, Long itemId);

    // 统计文件夹内项目总数
    long countByFolderId(Long folderId);

    // 检查用户是否收藏了某个项目（任何文件夹）
    @Query("SELECT COUNT(fi) > 0 FROM FavoriteItem fi WHERE fi.folder.userId = :userId AND fi.itemType = :itemType AND fi.itemId = :itemId")
    boolean existsByUserIdAndItemTypeAndItemId(@Param("userId") Long userId, @Param("itemType") String itemType, @Param("itemId") Long itemId);

    @Query("SELECT COUNT(fi) FROM FavoriteItem fi WHERE fi.folder.userId = :userId AND fi.itemType = :itemType AND fi.itemId = :itemId")
    long countByUserIdAndItemTypeAndItemId(@Param("userId") Long userId, @Param("itemType") String itemType, @Param("itemId") Long itemId);

    @Query("SELECT COUNT(fi) FROM FavoriteItem fi WHERE fi.itemType = :itemType AND fi.itemId = :itemId")
    long countByItemTypeAndItemId(@Param("itemType") String itemType, @Param("itemId") Long itemId);


}