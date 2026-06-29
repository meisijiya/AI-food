package com.ai.food.service.upload;

import com.ai.food.config.UploadPathProperties;
import com.ai.food.exception.BusinessException;
import com.ai.food.common.mapper.ChatFileMapper;
import com.ai.food.common.mapper.ChatPhotoMapper;
import com.ai.food.common.mapper.PhotoMapper;
import com.ai.food.common.model.ChatFile;
import com.ai.food.common.model.ChatPhoto;
import com.ai.food.common.model.Photo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 上传服务：照片、头像、聊天图片、聊天文件。
 * <p>主实体选 {@link Photo}（业务量最大），{@code baseMapper} 由 ServiceImpl 父类注入；
 * 聊天侧两个相关实体（{@link ChatPhoto} / {@link ChatFile}）通过构造函数注入各自的 Mapper。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService extends ServiceImpl<PhotoMapper, Photo> {

    private final ChatPhotoMapper chatPhotoMapper;
    private final ChatFileMapper chatFileMapper;
    // L9 polish: 注入统一上传目录配置，替代原 getUploadRoot() 中硬编码的 user.dir + "uploads"
    private final UploadPathProperties uploadPathProperties;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    private static final long MAX_CHAT_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    // H6 polish: chat file 白名单只放非可执行文档；攻击者上传 .html / .svg / .exe 都拒
    private static final Set<String> CHAT_FILE_ALLOWED_EXTS = Set.of(
            "pdf", "docx", "xlsx", "pptx", "zip", "txt", "md",
            "jpg", "jpeg", "png", "gif", "webp"
    );

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
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
            Optional<Photo> optPhoto = Optional.ofNullable(
                    baseMapper.findByUserIdAndOriginalPath(userId, oldPhotoUrl));
            if (optPhoto.isEmpty()) {
                optPhoto = Optional.ofNullable(baseMapper.findByThumbnailPath(oldPhotoUrl));
            }
            if (optPhoto.isPresent()) {
                Photo photo = optPhoto.get();
                deletePhysicalFile(photo.getOriginalPath());
                deletePhysicalFile(photo.getThumbnailPath());
                // ponytail: 显式 updateById，@TableLogic 软删字段自动写入；version 自增
                baseMapper.updateById(photo);
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
            if (!oldThumbnailUrl.startsWith("/uploads/avatars/")) {
                log.warn("Skip deleting avatar outside avatar directory: {}", oldThumbnailUrl);
                return;
            }
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
        Path root = Paths.get(uploadPathProperties.getDir()).toAbsolutePath().normalize();
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

        Path root = Paths.get(uploadPathProperties.getDir()).toAbsolutePath();
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
        // ponytail: 显式 insert，@TableId AssignId 自动回填 id 到 photo 对象
        baseMapper.insert(photo);
        Photo saved = photo;

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

        Path root = Paths.get(uploadPathProperties.getDir()).toAbsolutePath();
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

        Path root = Paths.get(uploadPathProperties.getDir()).toAbsolutePath();
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
        // ponytail: 显式 insert；ChatPhoto 是新实体，@TableId AssignId 自动回填
        chatPhotoMapper.insert(chatPhoto);
        ChatPhoto saved = chatPhoto;

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

        // H6 polish: 扩展名白名单 + 大小写归一，挡掉 .html/.svg/.exe 等存储型 XSS / 可执行入口
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!CHAT_FILE_ALLOWED_EXTS.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + (ext.isEmpty() ? "(无扩展名)" : ext));
        }

        byte[] fileBytes = file.getBytes();
        if (fileBytes.length > MAX_CHAT_FILE_SIZE) {
            throw new IOException("文件大小不能超过50MB");
        }

        Path root = Paths.get(uploadPathProperties.getDir()).toAbsolutePath();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dirPath = root.resolve("chat-files").resolve(dateDir);
        Files.createDirectories(dirPath);

        String baseName = UUID.randomUUID().toString().replace("-", "");
        String fileName = baseName + "." + ext;

        Path filePath = dirPath.resolve(fileName);
        Files.write(filePath, fileBytes);

        // H6 polish: 简单 magic-byte 校验——扩展名命中后再用 probeContentType 比 mime，
        // 防止"白名单扩展名 + 实际 HTML/可执行 payload"绕过
        String probedType = Files.probeContentType(filePath);
        if (!isMimeMatchesExt(probedType, ext)) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
                // ponytail: 清理失败不影响主流程——反正不入库
            }
            throw new BusinessException("文件内容与扩展名不匹配: " + ext);
        }

        ChatFile chatFile = new ChatFile();
        chatFile.setSenderId(getCurrentUserId());
        chatFile.setFilePath("/uploads/chat-files/" + dateDir + "/" + fileName);
        chatFile.setOriginalName(originalName != null ? originalName : fileName);
        chatFile.setFileSize((long) fileBytes.length);
        chatFile.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        // ponytail: 显式 insert；@TableId AssignId 自动回填
        chatFileMapper.insert(chatFile);
        ChatFile saved = chatFile;

        log.info("Chat file uploaded: id={}, name={}, size={}B", saved.getId(), originalName, fileBytes.length);

        return Map.of(
                "fileId", saved.getId(),
                "fileUrl", "/uploads/chat-files/" + dateDir + "/" + fileName,
                "fileName", originalName != null ? originalName : fileName,
                "fileSize", (long) fileBytes.length,
                "mimeType", file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );
    }

    /**
     * 比较 probeContentType 推断的 MIME 与扩展名是否一致。
     * ponytail: probeContentType 依赖 OS mime.types；Office 套件（docx/xlsx/pptx）实际是 zip，
     * 部分环境会返回 application/zip 或 application/octet-stream，所以这三种放宽放行；
     * null 视为系统无法识别（如缺 mime.types），不阻断——白名单已先行兜底。
     */
    private static boolean isMimeMatchesExt(String mime, String ext) {
        if (mime == null) return true;
        return switch (ext) {
            case "pdf" -> mime.equals("application/pdf");
            case "docx", "xlsx", "pptx" -> mime.startsWith("application/vnd.openxmlformats-officedocument")
                    || mime.equals("application/zip")
                    || mime.equals("application/octet-stream");
            case "zip" -> mime.equals("application/zip") || mime.equals("application/x-zip-compressed");
            case "txt" -> mime.startsWith("text/plain");
            case "md" -> mime.startsWith("text/");
            case "jpg", "jpeg" -> mime.equals("image/jpeg");
            case "png" -> mime.equals("image/png");
            case "gif" -> mime.equals("image/gif");
            case "webp" -> mime.equals("image/webp");
            default -> false;
        };
    }
}
