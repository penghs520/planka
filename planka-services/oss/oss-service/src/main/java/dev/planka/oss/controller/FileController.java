package dev.planka.oss.controller;

import dev.planka.common.result.Result;
import dev.planka.oss.plugin.FileCategory;
import dev.planka.oss.service.FileService;
import dev.planka.oss.api.dto.DownloadUrlResponse;
import dev.planka.oss.api.dto.FileDTO;
import dev.planka.oss.api.dto.PresignedUploadRequest;
import dev.planka.oss.api.dto.PresignedUploadResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 文件 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     *
     * @param file       文件
     * @param orgId      组织 ID
     * @param operatorId 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
     * @param category   文件类别
     */
    @PostMapping("/upload")
    public Result<FileDTO> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam("orgId") String orgId,
        @RequestParam("operatorId") String operatorId,
        @RequestParam("category") FileCategory category) {

        FileDTO result = fileService.upload(file, orgId, operatorId, category);
        return Result.success(result);
    }

    /**
     * 批量上传文件
     *
     * @param files      文件列表
     * @param orgId      组织 ID
     * @param operatorId 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
     * @param category   文件类别
     */
    @PostMapping("/upload/batch")
    public Result<List<FileDTO>> uploadBatch(
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam("orgId") String orgId,
        @RequestParam("operatorId") String operatorId,
        @RequestParam("category") FileCategory category) {

        List<FileDTO> results = fileService.uploadBatch(files, orgId, operatorId, category);
        return Result.success(results);
    }

    /**
     * 获取预签名上传 URL
     */
    @PostMapping("/presigned-upload-url")
    public Result<PresignedUploadResponse> getPresignedUploadUrl(@RequestBody PresignedUploadRequest request) {
        Optional<PresignedUploadResponse> result = fileService.getPresignedUploadUrl(request);
        return result.map(Result::success)
            .orElse(Result.failure("PRESIGNED_NOT_SUPPORTED", "当前存储插件不支持预签名上传"));
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}")
    public Result<FileDTO> getFile(@PathVariable String fileId) {
        Optional<FileDTO> result = fileService.getFile(fileId);
        return result.map(Result::success)
            .orElse(Result.failure("FILE_NOT_FOUND", "文件不存在"));
    }

    /**
     * 获取文件内容（用于图片等资源的直接访问）
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<?> getContent(@PathVariable String fileId) {
        Optional<FileDTO> fileOpt = fileService.getFile(fileId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<InputStream> inputStreamOpt = fileService.download(fileId);
        if (inputStreamOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileDTO file = fileOpt.get();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.getContentType()))
            .contentLength(file.getSize())
            .body(new InputStreamResource(inputStreamOpt.get()));
    }

    /**
     * 获取下载 URL
     */
    @GetMapping("/{fileId}/download-url")
    public Result<DownloadUrlResponse> getDownloadUrl(
        @PathVariable String fileId,
        @RequestParam(defaultValue = "3600") int expirationSeconds) {

        Optional<String> url = fileService.getDownloadUrl(fileId, Duration.ofSeconds(expirationSeconds));
        return url.map(u -> Result.success(new DownloadUrlResponse(u, expirationSeconds)))
            .orElse(Result.failure("FILE_NOT_FOUND", "文件不存在"));
    }

    /**
     * 下载文件
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<?> download(@PathVariable String fileId, HttpServletResponse response) {
        Optional<FileDTO> fileOpt = fileService.getFile(fileId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<InputStream> inputStreamOpt = fileService.download(fileId);
        if (inputStreamOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileDTO file = fileOpt.get();
        String encodedFileName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
            .contentType(MediaType.parseMediaType(file.getContentType()))
            .contentLength(file.getSize())
            .body(new InputStreamResource(inputStreamOpt.get()));
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public Result<Boolean> delete(@PathVariable String fileId) {
        boolean deleted = fileService.delete(fileId);
        if (deleted) {
            return Result.success(true);
        }
        return Result.failure("FILE_DELETE_FAILED", "文件不存在或删除失败");
    }
}
