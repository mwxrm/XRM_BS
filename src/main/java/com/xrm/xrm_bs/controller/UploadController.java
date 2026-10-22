package com.xrm.xrm_bs.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.path:uploads/images/}")
    private String uploadPath;

    @Value("${upload.url-prefix:/images/}")
    private String urlPrefix;

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("errno", 1);
            response.put("message", "文件不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + extension;

            // 关键修复：基于项目根目录的绝对路径
            Path projectDir = Paths.get(System.getProperty("user.dir"));
            Path uploadDir = projectDir.resolve(uploadPath).normalize();
            Files.createDirectories(uploadDir);

            File destFile = uploadDir.resolve(newFileName).toFile();
            file.transferTo(destFile);

            String fileUrl = urlPrefix + newFileName;

            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);

            response.put("errno", 0);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("errno", 1);
            response.put("message", "文件保存失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}