package dev.planka.oss.service;

import dev.planka.oss.config.OssPluginRegistry;
import dev.planka.oss.config.OssProperties;
import dev.planka.oss.entity.FileMeta;
import dev.planka.oss.repository.FileMetaRepository;
import dev.planka.oss.api.dto.FileDTO;
import dev.planka.oss.api.dto.PresignedUploadRequest;
import dev.planka.oss.api.dto.PresignedUploadResponse;
import dev.planka.oss.plugin.FileCategory;
import dev.planka.oss.plugin.OssPlugin;
import dev.planka.oss.plugin.UploadRequest;
import dev.planka.oss.plugin.UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final OssPluginRegistry pluginRegistry;
    private final OssProperties ossProperties;
    private final FileMetaRepository fileMetaRepository;

    /**
     * 上传文件
     *
     * @param file       文件
     * @param orgId      组织 ID
     * @param operatorId 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
     * @param category   文件类别
     */
    @Transactional
    public FileDTO upload(MultipartFile file, String orgId, String operatorId, FileCategory category) {
        OssPlugin plugin = getActivePlugin();

        try {
            UploadRequest request = UploadRequest.builder()
                .orgId(orgId)
                .operatorId(operatorId)
                .category(category)
                .originalName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .inputStream(file.getInputStream())
                .build();

            UploadResult result = plugin.getClient().upload(request);

            if (!result.isSuccess()) {
                throw new RuntimeException(result.getErrorMessage());
            }

            FileMeta fileMeta = new FileMeta();
            fileMeta.setId(UUID.randomUUID().toString());
            fileMeta.setOrgId(orgId);
            fileMeta.setOperatorId(operatorId);
            fileMeta.setCategory(category);
            fileMeta.setOriginalName(file.getOriginalFilename());
            fileMeta.setObjectKey(result.getObjectKey());
            fileMeta.setUrl(result.getUrl());
            fileMeta.setSize(result.getSize());
            fileMeta.setContentType(result.getContentType());
            fileMeta.setStoragePlugin(plugin.pluginId());
            fileMeta.setCreatedAt(LocalDateTime.now());

            fileMetaRepository.insert(fileMeta);

            return toDTO(fileMeta);
        } catch (IOException e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 批量上传文件
     *
     * @param files      文件列表
     * @param orgId      组织 ID
     * @param operatorId 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
     * @param category   文件类别
     */
    @Transactional
    public List<FileDTO> uploadBatch(List<MultipartFile> files, String orgId, String operatorId, FileCategory category) {
        return files.stream()
            .map(file -> upload(file, orgId, operatorId, category))
            .toList();
    }

    /**
     * 获取文件信息
     */
    public Optional<FileDTO> getFile(String fileId) {
        FileMeta fileMeta = fileMetaRepository.selectById(fileId);
        if (fileMeta == null) {
            return Optional.empty();
        }
        return Optional.of(toDTO(fileMeta));
    }

    /**
     * 下载文件
     */
    public Optional<InputStream> download(String fileId) {
        FileMeta fileMeta = fileMetaRepository.selectById(fileId);
        if (fileMeta == null) {
            return Optional.empty();
        }

        OssPlugin plugin = pluginRegistry.getPlugin(fileMeta.getStoragePlugin())
            .orElseThrow(() -> new RuntimeException("存储插件不存在: " + fileMeta.getStoragePlugin()));

        return plugin.getClient().download(fileMeta.getObjectKey());
    }

    /**
     * 获取下载 URL
     */
    public Optional<String> getDownloadUrl(String fileId, Duration expiration) {
        FileMeta fileMeta = fileMetaRepository.selectById(fileId);
        if (fileMeta == null) {
            return Optional.empty();
        }

        OssPlugin plugin = pluginRegistry.getPlugin(fileMeta.getStoragePlugin())
            .orElseThrow(() -> new RuntimeException("存储插件不存在: " + fileMeta.getStoragePlugin()));

        if (plugin.supportsPresignedUrl()) {
            return plugin.getClient().generatePresignedDownloadUrl(fileMeta.getObjectKey(), expiration);
        }

        return Optional.of(fileMeta.getUrl());
    }

    /**
     * 删除文件
     */
    @Transactional
    public boolean delete(String fileId) {
        FileMeta fileMeta = fileMetaRepository.selectById(fileId);
        if (fileMeta == null) {
            return false;
        }

        OssPlugin plugin = pluginRegistry.getPlugin(fileMeta.getStoragePlugin())
            .orElseThrow(() -> new RuntimeException("存储插件不存在: " + fileMeta.getStoragePlugin()));

        boolean deleted = plugin.getClient().delete(fileMeta.getObjectKey());
        if (deleted) {
            fileMetaRepository.deleteById(fileId);
        }
        return deleted;
    }

    /**
     * 获取预签名上传 URL
     */
    public Optional<PresignedUploadResponse> getPresignedUploadUrl(PresignedUploadRequest request) {
        OssPlugin plugin = getActivePlugin();

        if (!plugin.supportsPresignedUrl()) {
            return Optional.empty();
        }

        String objectKey = generateObjectKey(request.getOrgId(), request.getCategory(), request.getFileName());
        Duration expiration = Duration.ofSeconds(request.getExpirationSeconds());

        return plugin.getClient()
            .generatePresignedUploadUrl(objectKey, request.getContentType(), expiration)
            .map(url -> PresignedUploadResponse.builder()
                .uploadUrl(url)
                .objectKey(objectKey)
                .expiresIn(request.getExpirationSeconds())
                .build());
    }

    private OssPlugin getActivePlugin() {
        return pluginRegistry.getPlugin(ossProperties.getActivePlugin())
            .orElseThrow(() -> new RuntimeException("存储插件不存在: " + ossProperties.getActivePlugin()));
    }

    private String generateObjectKey(String orgId, FileCategory category, String fileName) {
        String categoryPath = switch (category) {
            case ATTACHMENT -> "attachments";
            case AVATAR -> "avatars";
            case ORG_LOGO -> "logos";
            case COMMENT_IMAGE -> "comments";
            case DESCRIPTION_IMAGE -> "descriptions";
        };
        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(fileName);
        return String.format("%s/%s/%s%s", orgId, categoryPath, uuid, extension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex);
        }
        return "";
    }

    private FileDTO toDTO(FileMeta fileMeta) {
        // URL 格式改为 /api/v1/files/{fileId}/content，便于前端从 URL 解析 fileId
        String contentUrl = "/api/v1/files/" + fileMeta.getId() + "/content";
        return FileDTO.builder()
            .id(fileMeta.getId())
            .orgId(fileMeta.getOrgId())
            .operatorId(fileMeta.getOperatorId())
            .category(fileMeta.getCategory())
            .originalName(fileMeta.getOriginalName())
            .url(contentUrl)
            .size(fileMeta.getSize())
            .contentType(fileMeta.getContentType())
            .createdAt(fileMeta.getCreatedAt())
            .build();
    }
}
