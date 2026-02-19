package dev.planka.oss.plugin.minio;

import dev.planka.oss.plugin.FileCategory;
import dev.planka.oss.plugin.OssClient;
import dev.planka.oss.plugin.UploadRequest;
import dev.planka.oss.plugin.UploadResult;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储客户端
 */
@Slf4j
public class MinioOssClient implements OssClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final MinioClient minioClient;
    private final String bucket;
    private final String endpoint;

    public MinioOssClient(MinioOssProperties properties) {
        this.endpoint = properties.getEndpoint();
        this.bucket = properties.getBucket();
        this.minioClient = MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .build();
        initBucket();
    }

    private void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build());
                log.info("创建 MinIO 存储桶: {}", bucket);
            }
        } catch (Exception e) {
            log.error("初始化 MinIO 存储桶失败: {}", bucket, e);
            throw new RuntimeException("初始化 MinIO 存储桶失败", e);
        }
    }

    @Override
    public UploadResult upload(UploadRequest request) {
        try {
            String objectKey = generateObjectKey(request);

            try (InputStream inputStream = request.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, request.getSize(), -1)
                    .contentType(request.getContentType())
                    .build());
            }

            String url = buildUrl(objectKey);
            log.debug("文件上传成功: objectKey={}, url={}", objectKey, url);

            return UploadResult.success(objectKey, url, request.getSize(), request.getContentType());
        } catch (Exception e) {
            log.error("文件上传失败: {}", request.getOriginalName(), e);
            return UploadResult.failure("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Optional<InputStream> download(String objectKey) {
        try {
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
            return Optional.of(inputStream);
        } catch (Exception e) {
            log.error("文件下载失败: {}", objectKey, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
            log.debug("文件删除成功: {}", objectKey);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectKey, e);
            return false;
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<String> generatePresignedUploadUrl(String objectKey, String contentType, Duration expiration) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucket)
                .object(objectKey)
                .expiry((int) expiration.getSeconds(), TimeUnit.SECONDS)
                .build());
            return Optional.of(url);
        } catch (Exception e) {
            log.error("生成预签名上传 URL 失败: {}", objectKey, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> generatePresignedDownloadUrl(String objectKey, Duration expiration) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectKey)
                .expiry((int) expiration.getSeconds(), TimeUnit.SECONDS)
                .build());
            return Optional.of(url);
        } catch (Exception e) {
            log.error("生成预签名下载 URL 失败: {}", objectKey, e);
            return Optional.empty();
        }
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
        return endpoint + "/" + bucket + "/" + objectKey;
    }
}
