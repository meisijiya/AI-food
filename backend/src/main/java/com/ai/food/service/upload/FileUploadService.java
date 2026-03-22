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

    public Map<String, Object> uploadPhoto(MultipartFile file, Long userId, String sessionId) throws IOException {
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
}
