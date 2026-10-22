package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.FavoriteFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteFolderRepository extends JpaRepository<FavoriteFolder, Long> {
    List<FavoriteFolder> findByUserId(Long userId);
    Optional<FavoriteFolder> findByUserIdAndId(Long userId, Long folderId);
    Optional<FavoriteFolder> findByUserIdAndName(Long userId, String name);
    boolean existsByUserIdAndName(Long userId, String name);
    long countByUserId(Long userId);  // 用于限制文件夹数量
}