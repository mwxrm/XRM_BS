package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.*;
import com.xrm.xrm_bs.entity.Article;
import com.xrm.xrm_bs.entity.FavoriteFolder;
import com.xrm.xrm_bs.entity.FavoriteItem;
import com.xrm.xrm_bs.entity.Setting;
import com.xrm.xrm_bs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteFolderRepository folderRepository;
    private final FavoriteItemRepository itemRepository;
    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final NotificationService notificationService;
    

    private final UserFollowRepository followRepository;
//    private final UserRepository userRepository;

    private final NotificationRepository notificationRepository;
    private final UserBehaviorLogService behaviorLogService;

    private static final int MAX_FOLDERS_PER_USER = 100;
//    private static final int MAX_ITEMS_PER_FOLDER = 1000;


    // ------------------ 文件夹管理 ------------------

    @Transactional
    public FolderResponse createFolder(Long userId, CreateFolderRequest request) {
        // 检查数量限制
        long folderCount = folderRepository.countByUserId(userId);
        if (folderCount >= MAX_FOLDERS_PER_USER) {
            throw new RuntimeException("文件夹数量已达上限（100个）");
        }

        // 检查名称是否重复
        if (folderRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new RuntimeException("文件夹名称已存在");
        }

        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setName(request.getName());
        folder.setDescription(request.getDescription());
//        folder.setType(null);
        folder = folderRepository.save(folder);

        return FolderResponse.fromFolder(folder, 0);
    }

    @Transactional
    public FolderResponse updateFolder(Long userId, Long folderId, UpdateFolderRequest request) {
        FavoriteFolder folder = folderRepository.findByUserIdAndId(userId, folderId)
                .orElseThrow(() -> new RuntimeException("文件夹不存在"));

        if (request.getName() != null && !request.getName().equals(folder.getName())) {
            if (folderRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new RuntimeException("文件夹名称已存在");
            }
            folder.setName(request.getName());
        }
        if (request.getDescription() != null) {
            folder.setDescription(request.getDescription());
        }

        folder = folderRepository.save(folder);
        int itemCount = (int) itemRepository.countByFolderId(folderId);
        return FolderResponse.fromFolder(folder, itemCount);
    }

    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        FavoriteFolder folder = folderRepository.findByUserIdAndId(userId, folderId)
                .orElseThrow(() -> new RuntimeException("文件夹不存在"));
        folderRepository.delete(folder);
    }

    // 添加收藏（通用）
    public void addFavorite(Long userId, String itemType, Long itemId) {
        // 1. 验证内容存在
        if ("article".equals(itemType)) {
            if (!articleRepository.existsById(itemId)) throw new RuntimeException("文章不存在");
        } else if ("setting".equals(itemType)) {
            if (!settingRepository.existsById(itemId)) throw new RuntimeException("设定不存在");
        } else {
            throw new RuntimeException("无效的收藏类型");
        }

        // 2. 获取或创建默认文件夹
        FavoriteFolder defaultFolder = getOrCreateDefaultFolder(userId);

        // 3. 添加收藏项
        addItemToFolder(userId, defaultFolder.getId(), itemType, itemId);

        // 假设方法签名为 public void addFavorite(Long userId, String itemType, Long itemId)
        // 添加收藏成功后的通知
        Long contentAuthorId = null;
        String title = "";
        if ("article".equals(itemType)) {
            Article article = articleRepository.findById(itemId).orElse(null);
            if (article != null) {
                contentAuthorId = article.getAuthor().getId();
                title = article.getTitle();
            }
        } else if ("setting".equals(itemType)) {
            Setting setting = settingRepository.findById(itemId).orElse(null);
            if (setting != null) {
                contentAuthorId = setting.getAuthor().getId();
                title = setting.getTitle();
            }
        }
        if (contentAuthorId != null && !contentAuthorId.equals(userId)) {
            String content = "收藏了你的" + ("article".equals(itemType) ? "文章" : "设定") + "《" + title + "》";
            notificationService.createNotification(
                    contentAuthorId,
                    "favorite_" + itemType,
                    userId, itemId,
                    itemType,
                    content
            );
        }
        behaviorLogService.log(userId, itemId, itemType, "favorite");
    }

    // 将项目添加到指定文件夹
    public void addItemToFolder(Long userId, Long folderId, String itemType, Long itemId) {
        FavoriteFolder folder = folderRepository.findByUserIdAndId(userId, folderId)
                .orElseThrow(() -> new RuntimeException("文件夹不存在"));
        if (itemRepository.existsByFolderIdAndItemTypeAndItemId(folderId, itemType, itemId)) {
            throw new RuntimeException("该项目已在收藏夹中");
        }
        FavoriteItem item = new FavoriteItem();
        item.setFolder(folder);
        item.setItemType(itemType);
        item.setItemId(itemId);
        itemRepository.save(item);

        long count = itemRepository.countByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
        if (count == 1) {
            // 2. 【新增】发送通知逻辑
            if ("article".equals(itemType)) {
                articleRepository.findById(itemId).ifPresent(article -> {
                    Long authorId = article.getAuthor().getId();
                    if (!authorId.equals(userId)) {
                        notificationService.createNotification(
                                authorId,
                                "favorite_article",
                                userId,
                                itemId,
                                "article",
                                "收藏了你的文章：" + article.getTitle()
                        );
                    }
                });
            } else if ("setting".equals(itemType)) {
                settingRepository.findById(itemId).ifPresent(setting -> {
                    Long authorId = setting.getAuthor().getId();
                    if (!authorId.equals(userId)) {
                        notificationService.createNotification(
                                authorId,
                                "favorite_setting",
                                userId,
                                itemId,
                                "setting",
                                "收藏了你的设定：" + setting.getTitle()
                        );
                    }
                });
            }
        }
    }

    // 移除收藏
    public void removeFavorite(Long userId, String itemType, Long itemId) {
        FavoriteFolder defaultFolder = getOrCreateDefaultFolder(userId);
        removeItemFromFolder(userId, defaultFolder.getId(), itemType, itemId);
    }

    // 移除文件夹中的项目
    public void removeItemFromFolder(Long userId, Long folderId, String itemType, Long itemId) {
        // 验证文件夹归属
        folderRepository.findByUserIdAndId(userId, folderId)
                .orElseThrow(() -> new RuntimeException("文件夹不存在"));
        itemRepository.deleteByFolderIdAndItemTypeAndItemId(folderId, itemType, itemId);
    }

    // 查询用户收藏夹（不区分类型）
    public List<FolderResponse> getUserFolders(Long userId) {
        List<FavoriteFolder> folders = folderRepository.findByUserId(userId);
        return folders.stream()
                .map(f -> FolderResponse.fromFolder(f, (int) itemRepository.countByFolderId(f.getId())))
                .collect(Collectors.toList());
    }

    // 查询文件夹内项目（分页，可指定类型）
    public Page<FavoriteItemResponse> getFolderItems(Long userId, Long folderId, String itemType, Pageable pageable) {
//        FavoriteFolder folder = folderRepository.findByUserIdAndId(userId, folderId)
//                .orElseThrow(() -> new RuntimeException("文件夹不存在"));
        Page<FavoriteItem> itemPage;
        if (itemType != null) {
            itemPage = itemRepository.findByFolderIdAndItemType(folderId, itemType, pageable);
        } else {
            itemPage = itemRepository.findByFolderId(folderId, pageable);
        }
        // 转换为响应，根据 itemType 加载对应的实体
        List<FavoriteItemResponse> responses = itemPage.getContent().stream()
                .map(item -> {
                    if ("article".equals(item.getItemType())) {
                        Article article = articleRepository.findById(item.getItemId()).orElse(null);
                        FavoriteItemResponse resp = new FavoriteItemResponse();
                        resp.setId(item.getId());
                        resp.setItemId(item.getItemId());
                        resp.setItemType("article");
                        resp.setFavoritedAt(item.getCreatedAt());
                        if (article == null) {
                            // 硬删除（但我们已经软删除，理论上不会出现 null，但兼容）
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
                    } else if ("setting".equals(item.getItemType())) {
                        Setting setting = settingRepository.findById(item.getItemId()).orElse(null);
                        FavoriteItemResponse resp = new FavoriteItemResponse();
                        resp.setId(item.getId());
                        resp.setItemId(item.getItemId());
                        resp.setItemType("setting");
                        resp.setFavoritedAt(item.getCreatedAt());
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
        return new PageImpl<>(responses, pageable, itemPage.getTotalElements());
    }



    // 获取用户对某个项目的收藏状态（任何文件夹）
    public boolean isItemFavorited(Long userId, String itemType, Long itemId) {
        return itemRepository.existsByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
    }

    // 获取用户所有文件夹中该项目是否已存在（用于前端显示）
    public List<FolderWithStatusResponse> getUserFoldersWithItemStatus(Long userId, String itemType, Long itemId) {
        List<FavoriteFolder> folders = folderRepository.findByUserId(userId);
        return folders.stream().map(folder -> {
            int itemCount = (int) itemRepository.countByFolderId(folder.getId());
            FolderWithStatusResponse response = new FolderWithStatusResponse();
            response.setId(folder.getId());
            response.setName(folder.getName());
            response.setDescription(folder.getDescription());
            response.setCreatedAt(folder.getCreatedAt());
            response.setUpdatedAt(folder.getUpdatedAt());
            response.setItemCount(itemCount);
            boolean hasItem = itemRepository.existsByFolderIdAndItemTypeAndItemId(folder.getId(), itemType, itemId);
            response.setHasItem(hasItem);
            return response;
        }).collect(Collectors.toList());
    }

    private FavoriteFolder getOrCreateDefaultFolder(Long userId) {
        return folderRepository.findByUserIdAndName(userId, "默认收藏")
                .orElseGet(() -> {
                    FavoriteFolder folder = new FavoriteFolder();
                    folder.setUserId(userId);
                    folder.setName("默认收藏");
                    folder.setDescription("系统默认收藏夹");
                    return folderRepository.save(folder);
                });
    }
}