package com.ai.food.service.upload;

import com.ai.food.model.ChatFile;
import com.ai.food.model.ChatPhoto;
import com.ai.food.model.Photo;
import com.ai.food.repository.ChatFileRepository;
import com.ai.food.repository.ChatPhotoRepository;
import com.ai.food.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ChatPhotoRepository chatPhotoRepository;
    private final ChatFileRepository chatFileRepository;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    private static final long MAX_CHAT_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }

    private Path getUploadRoot() {
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
    }

    /**
     * 删除物理文件（原图 + 缩略图推导删除）
     */
    public void deletePhysicalFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path filePath = resolveUploadPath(relativePath);
            if (filePath == null) {
                log.warn("Rejected physical file delete outside upload root: {}", relativePath);
                return;
            }
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
                log.warn("Skip deleting old photo without owned database record: userId={}, path={}", userId, oldPhotoUrl);
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

    /**
     * 将相对上传路径规范化为 uploads 根目录下的绝对路径，越界时返回 null。
     */
    private Path resolveUploadPath(String relativePath) {
        Path root = getUploadRoot().normalize();
        String relative = relativePath.startsWith("/uploads") ? relativePath.substring("/uploads".length()) : relativePath;
        String sanitized = relative.startsWith("/") ? relative.substring(1) : relative;
        Path resolved = root.resolve(sanitized).normalize();
        if (!resolved.startsWith(root)) {
            return null;
        }
        return resolved;
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

    // ==================== 聊天照片上传 ====================

    public Map<String, Object> uploadChatPhoto(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("文件为空");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("仅支持图片格式");
        }

        byte[] originalBytes = file.getBytes();
        if (originalBytes.length > 10 * 1024 * 1024) {
            throw new IOException("图片大小不能超过10MB");
        }

        Path root = getUploadRoot();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dirPath = root.resolve("chat-photos").resolve(dateDir);
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

        Path originalPath = dirPath.resolve(fileName);
        Files.write(originalPath, originalBytes);

        Path thumbPath = dirPath.resolve(thumbName);
        try {
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .width(400)
                    .outputFormat("jpg")
                    .outputQuality(0.7)
                    .toFile(thumbPath.toFile());
        } catch (Exception e) {
            log.warn("Chat photo thumbnail failed: {}", e.getMessage());
            Files.copy(originalPath, thumbPath);
        }

        long thumbSize = Files.size(thumbPath);

        ChatPhoto chatPhoto = new ChatPhoto();
        chatPhoto.setSenderId(getCurrentUserId());
        chatPhoto.setOriginalPath("/uploads/chat-photos/" + dateDir + "/" + fileName);
        chatPhoto.setThumbnailPath("/uploads/chat-photos/" + dateDir + "/" + thumbName);
        chatPhoto.setFileName(originalName != null ? originalName : fileName);
        chatPhoto.setOriginalSize((long) originalBytes.length);
        chatPhoto.setThumbnailSize(thumbSize);
        chatPhoto.setMimeType(contentType);
        ChatPhoto saved = chatPhotoRepository.save(chatPhoto);

        log.info("Chat photo uploaded: id={}, original={}B, thumb={}B", saved.getId(), originalBytes.length, thumbSize);

        return Map.of(
                "photoId", saved.getId(),
                "originalUrl", "/uploads/chat-photos/" + dateDir + "/" + fileName,
                "thumbnailUrl", "/uploads/chat-photos/" + dateDir + "/" + thumbName,
                "fileName", originalName != null ? originalName : fileName,
                "originalSize", (long) originalBytes.length,
                "thumbnailSize", thumbSize,
                "mimeType", contentType
        );
    }

    // ==================== 聊天文件上传 ====================

    public Map<String, Object> uploadChatFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("文件为空");

        byte[] fileBytes = file.getBytes();
        if (fileBytes.length > MAX_CHAT_FILE_SIZE) {
            throw new IOException("文件大小不能超过50MB");
        }

        Path root = getUploadRoot();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dirPath = root.resolve("chat-files").resolve(dateDir);
        Files.createDirectories(dirPath);

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String baseName = UUID.randomUUID().toString().replace("-", "");
        String fileName = baseName + ext;

        Path filePath = dirPath.resolve(fileName);
        Files.write(filePath, fileBytes);

        ChatFile chatFile = new ChatFile();
        chatFile.setSenderId(getCurrentUserId());
        chatFile.setFilePath("/uploads/chat-files/" + dateDir + "/" + fileName);
        chatFile.setOriginalName(originalName != null ? originalName : fileName);
        chatFile.setFileSize((long) fileBytes.length);
        chatFile.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        ChatFile saved = chatFileRepository.save(chatFile);

        log.info("Chat file uploaded: id={}, name={}, size={}B", saved.getId(), originalName, fileBytes.length);

        return Map.of(
                "fileId", saved.getId(),
                "fileUrl", "/uploads/chat-files/" + dateDir + "/" + fileName,
                "fileName", originalName != null ? originalName : fileName,
                "fileSize", (long) fileBytes.length,
                "mimeType", file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );
    }
}
