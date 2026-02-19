package dev.planka.oss.plugin.local;

import dev.planka.oss.plugin.FileCategory;
import dev.planka.oss.plugin.OssClient;
import dev.planka.oss.plugin.UploadRequest;
import dev.planka.oss.plugin.UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * 本地文件系统存储客户端
 */
@Slf4j
public class LocalOssClient implements OssClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final String basePath;
    private final String baseUrl;

    public LocalOssClient(LocalOssProperties properties) {
        this.basePath = expandPath(properties.getBasePath());
        this.baseUrl = properties.getBaseUrl();
        initBaseDir();
    }

    /**
     * 展开路径中的 ~ 为用户主目录
     */
    private String expandPath(String path) {
        if (path != null && path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private void initBaseDir() {
        try {
            Path path = Paths.get(basePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建本地存储目录: {}", basePath);
            }
        } catch (IOException e) {
            log.error("创建本地存储目录失败: {}", basePath, e);
            throw new RuntimeException("创建本地存储目录失败", e);
        }
    }

    @Override
    public UploadResult upload(UploadRequest request) {
        try {
            String objectKey = generateObjectKey(request);
            Path filePath = Paths.get(basePath, objectKey);

            // 确保父目录存在
            Files.createDirectories(filePath.getParent());

            // 写入文件
            try (InputStream inputStream = request.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = buildUrl(objectKey);
            log.debug("文件上传成功: objectKey={}, url={}", objectKey, url);

            return UploadResult.success(objectKey, url, request.getSize(), request.getContentType());
        } catch (IOException e) {
            log.error("文件上传失败: {}", request.getOriginalName(), e);
            return UploadResult.failure("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Optional<InputStream> download(String objectKey) {
        try {
            Path filePath = Paths.get(basePath, objectKey);
            if (Files.exists(filePath)) {
                return Optional.of(Files.newInputStream(filePath));
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("文件下载失败: {}", objectKey, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String objectKey) {
        try {
            Path filePath = Paths.get(basePath, objectKey);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("文件删除成功: {}", objectKey);
            }
            return deleted;
        } catch (IOException e) {
            log.error("文件删除失败: {}", objectKey, e);
            return false;
        }
    }

    @Override
    public boolean exists(String objectKey) {
        Path filePath = Paths.get(basePath, objectKey);
        return Files.exists(filePath);
    }

    @Override
    public Optional<String> generatePresignedUploadUrl(String objectKey, String contentType, Duration expiration) {
        // 本地存储不支持预签名 URL
        return Optional.empty();
    }

    @Override
    public Optional<String> generatePresignedDownloadUrl(String objectKey, Duration expiration) {
        // 本地存储直接返回静态文件 URL
        return Optional.of(buildUrl(objectKey));
    }

    private String generateObjectKey(UploadRequest request) {
        if (StringUtils.isNotBlank(request.getCustomObjectKey())) {
            return request.getCustomObjectKey();
        }

        String categoryPath = getCategoryPath(request.getCategory());
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String fileExtension = getFileExtension(request.getOriginalName());
        String fileName = UUID.randomUUID().toString() + fileExtension;

        return String.format("%s/%s/%s/%s", request.getOrgId(), categoryPath, datePath, fileName);
    }

    private String getCategoryPath(FileCategory category) {
        return switch (category) {
            case ATTACHMENT -> "attachments";
            case AVATAR -> "avatars";
            case ORG_LOGO -> "logos";
            case COMMENT_IMAGE -> "comments";
            case DESCRIPTION_IMAGE -> "descriptions";
        };
    }

    private String getFileExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex);
        }
        return "";
    }

    private String buildUrl(String objectKey) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/" + objectKey;
    }
}
