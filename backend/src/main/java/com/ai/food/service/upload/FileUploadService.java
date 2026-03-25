package com.ai.food.service.upload;

import com.ai.food.model.Photo;
import com.ai.food.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final PhotoRepository photoRepository;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    private Path getUploadRoot() {
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
    }

    /**
     * 删除物理文件（原图 + 缩略图推导删除）
     */
    public void deletePhysicalFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path root = getUploadRoot();
            String relative = relativePath.startsWith("/uploads") ? relativePath.substring("/uploads".length()) : relativePath;
            Path filePath = root.resolve(relative.startsWith("/") ? relative.substring(1) : relative);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete physical file: {}", relativePath, e);
        }
    }

    /**
     * 删除旧照片：原图 + 缩略图，同时软删除 Photo 记录
     */
    public void deleteOldPhoto(Long userId, String oldPhotoUrl) {
        if (oldPhotoUrl == null || oldPhotoUrl.isBlank()) return;
        try {
            // 尝试通过 originalPath 或 thumbnailPath 查找 Photo 记录
            var optPhoto = photoRepository.findByUserIdAndOriginalPath(userId, oldPhotoUrl);
            if (optPhoto.isEmpty()) {
                optPhoto = photoRepository.findByThumbnailPath(oldPhotoUrl);
            }
            if (optPhoto.isPresent()) {
                Photo photo = optPhoto.get();
                deletePhysicalFile(photo.getOriginalPath());
                deletePhysicalFile(photo.getThumbnailPath());
                photo.setIsDeleted(true);
                photoRepository.save(photo);
                log.info("Soft-deleted old photo id={} for user {}", photo.getId(), userId);
            } else {
                // 没有 Photo 记录，只删文件，尝试推导配对路径
                deletePhysicalFile(oldPhotoUrl);
                String paired = isThumbnailPath(oldPhotoUrl) ? deriveOriginalPath(oldPhotoUrl) : deriveThumbnailPath(oldPhotoUrl);
                deletePhysicalFile(paired);
            }
        } catch (Exception e) {
            log.warn("Failed to delete old photo: {}", oldPhotoUrl, e);
        }
    }

    /**
     * 删除旧头像：原图 + 缩略图
     */
    public void deleteOldAvatar(String oldThumbnailUrl) {
        if (oldThumbnailUrl == null || oldThumbnailUrl.isBlank()) return;
        try {
            // thumbnailUrl 格式: /uploads/avatars/xxx_thumb.jpg → 原图路径推导
            String originalUrl = deriveOriginalPath(oldThumbnailUrl);
            deletePhysicalFile(oldThumbnailUrl);
            deletePhysicalFile(originalUrl);
            log.info("Deleted old avatar files: thumb={}", oldThumbnailUrl);
        } catch (Exception e) {
            log.warn("Failed to delete old avatar: {}", oldThumbnailUrl, e);
        }
    }

    private String deriveThumbnailPath(String originalPath) {
        // /uploads/photos/20260322/xxx.jpg → /uploads/photos/20260322/xxx_thumb.jpg
        int dot = originalPath.lastIndexOf('.');
        if (dot > 0) return originalPath.substring(0, dot) + "_thumb.jpg";
        return originalPath + "_thumb.jpg";
    }

    private String deriveOriginalPath(String thumbnailPath) {
        // /uploads/avatars/xxx_thumb.jpg → /uploads/avatars/xxx.jpg
        if (thumbnailPath.contains("_thumb.")) {
            return thumbnailPath.replace("_thumb.", ".");
        }
        return thumbnailPath;
    }

    private boolean isThumbnailPath(String path) {
        return path != null && path.contains("_thumb.");
    }

    public Map<String, Object> uploadPhoto(MultipartFile file, Long userId, String sessionId, String oldPhotoUrl) throws IOException {
        // 替换照片时先删除旧文件
        if (oldPhotoUrl != null && !oldPhotoUrl.isBlank()) {
            deleteOldPhoto(userId, oldPhotoUrl);
        }

        if (file.isEmpty()) throw new IOException("文件为空");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("仅支持图片格式");
        }

        byte[] originalBytes = file.getBytes();
        if (originalBytes.length > 10 * 1024 * 1024) {
            throw new IOException("文件大小不能超过10MB");
        }

        Path root = getUploadRoot();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dirPath = root.resolve("photos").resolve(dateDir);
        Files.createDirectories(dirPath);

        String originalName = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            String e = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            if (e.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) ext = e;
        }
        String baseName = UUID.randomUUID().toString().replace("-", "");
        String fileName = baseName + ext;
        String thumbName = baseName + "_thumb.jpg";

        // 保存原图
        Path originalPath = dirPath.resolve(fileName);
        Files.write(originalPath, originalBytes);

        // 生成缩略图（400px 宽，JPEG 质量 0.7）
        Path thumbPath = dirPath.resolve(thumbName);
        try {
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .width(400)
                    .outputFormat("jpg")
                    .outputQuality(0.7)
                    .toFile(thumbPath.toFile());
        } catch (Exception e) {
            log.warn("Thumbnail generation failed, using original: {}", e.getMessage());
            Files.copy(originalPath, thumbPath);
        }

        long thumbSize = Files.size(thumbPath);

        // 保存到 photo 表
        Photo photo = new Photo();
        photo.setUserId(userId);
        photo.setOriginalPath("/uploads/photos/" + dateDir + "/" + fileName);
        photo.setThumbnailPath("/uploads/photos/" + dateDir + "/" + thumbName);
        photo.setRelatedSessionId(sessionId);
        photo.setFileName(originalName);
        photo.setOriginalSize((long) originalBytes.length);
        photo.setThumbnailSize(thumbSize);
        photo.setMimeType(contentType);
        Photo saved = photoRepository.save(photo);

        log.info("Photo uploaded: original={}B, thumb={}B", originalBytes.length, thumbSize);

        return Map.of(
                "photoId", saved.getId(),
                "originalUrl", saved.getOriginalPath(),
                "thumbnailUrl", saved.getThumbnailPath()
        );
    }

    public Map<String, String> uploadAvatar(MultipartFile file, String oldThumbnailUrl) throws IOException {
        // 替换头像时先删除旧文件
        if (oldThumbnailUrl != null && !oldThumbnailUrl.isBlank()) {
            deleteOldAvatar(oldThumbnailUrl);
        }

        if (file.isEmpty()) throw new IOException("文件为空");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("仅支持图片格式");
        }

        byte[] originalBytes = file.getBytes();
        if (originalBytes.length > 5 * 1024 * 1024) {
            throw new IOException("头像文件大小不能超过5MB");
        }

        Path root = getUploadRoot();
        Path dirPath = root.resolve("avatars");
        Files.createDirectories(dirPath);

        String originalName = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            String e = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            if (e.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) ext = e;
        }
        String baseName = UUID.randomUUID().toString().replace("-", "");
        String fileName = baseName + ext;
        String thumbName = baseName + "_thumb.jpg";

        // 保存原图
        Path originalPath = dirPath.resolve(fileName);
        Files.write(originalPath, originalBytes);

        // 生成缩略图（120px 正方形裁剪，JPEG 质量 0.8）
        Path thumbPath = dirPath.resolve(thumbName);
        try {
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(120, 120)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toFile(thumbPath.toFile());
        } catch (Exception e) {
            log.warn("Avatar thumbnail generation failed, using original: {}", e.getMessage());
            Files.copy(originalPath, thumbPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Avatar uploaded: original={}B", originalBytes.length);

        return Map.of(
                "originalUrl", "/uploads/avatars/" + fileName,
                "thumbnailUrl", "/uploads/avatars/" + thumbName
        );
    }
}
