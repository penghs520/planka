package dev.planka.oss.api;

import dev.planka.common.result.Result;
import dev.planka.oss.plugin.FileCategory;
import dev.planka.oss.api.dto.DownloadUrlResponse;
import dev.planka.oss.api.dto.FileDTO;
import dev.planka.oss.api.dto.PresignedUploadRequest;
import dev.planka.oss.api.dto.PresignedUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * OSS 服务 Feign Client
 */
@FeignClient(name = "oss-service", path = "/api/v1/files")
public interface OssFeignClient {

    /**
     * 上传文件
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<FileDTO> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam("orgId") String orgId,
        @RequestParam("userId") String userId,
        @RequestParam("category") FileCategory category
    );

    /**
     * 批量上传文件
     */
    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<List<FileDTO>> uploadBatch(
        @RequestPart("files") List<MultipartFile> files,
        @RequestParam("orgId") String orgId,
        @RequestParam("userId") String userId,
        @RequestParam("category") FileCategory category
    );

    /**
     * 获取预签名上传 URL
     */
    @PostMapping("/presigned-upload-url")
    Result<PresignedUploadResponse> getPresignedUploadUrl(@RequestBody PresignedUploadRequest request);

    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}")
    Result<FileDTO> getFile(@PathVariable("fileId") String fileId);

    /**
     * 获取下载 URL
     */
    @GetMapping("/{fileId}/download-url")
    Result<DownloadUrlResponse> getDownloadUrl(
        @PathVariable("fileId") String fileId,
        @RequestParam(value = "expirationSeconds", defaultValue = "3600") int expirationSeconds
    );

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    Result<Boolean> delete(@PathVariable("fileId") String fileId);
}
