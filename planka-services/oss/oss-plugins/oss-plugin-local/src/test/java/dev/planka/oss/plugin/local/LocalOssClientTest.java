package dev.planka.oss.plugin.local;

import dev.planka.oss.plugin.FileCategory;
import dev.planka.oss.plugin.UploadRequest;
import dev.planka.oss.plugin.UploadResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalOssClientTest {

    @TempDir
    Path tempDir;

    private LocalOssClient client;

    @BeforeEach
    void setUp() {
        LocalOssProperties properties = new LocalOssProperties();
        properties.setBasePath(tempDir.toString());
        properties.setBaseUrl("/files");
        client = new LocalOssClient(properties);
    }

    @AfterEach
    void tearDown() {
        // TempDir 会自动清理
    }

    @Test
    void upload_shouldSaveFileAndReturnResult() {
        // Given
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        UploadRequest request = UploadRequest.builder()
            .orgId("org-1")
            .operatorId("operator-1")
            .category(FileCategory.ATTACHMENT)
            .originalName("test.txt")
            .contentType("text/plain")
            .size(content.length())
            .inputStream(inputStream)
            .build();

        // When
        UploadResult result = client.upload(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObjectKey()).isNotBlank();
        assertThat(result.getUrl()).startsWith("/files/");
        assertThat(result.getSize()).isEqualTo(content.length());
        assertThat(result.getContentType()).isEqualTo("text/plain");
    }

    @Test
    void upload_withCustomObjectKey_shouldUseCustomKey() {
        // Given
        String content = "Custom key test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String customKey = "custom/path/file.txt";
        UploadRequest request = UploadRequest.builder()
            .orgId("org-1")
            .operatorId("operator-1")
            .category(FileCategory.ATTACHMENT)
            .originalName("test.txt")
            .contentType("text/plain")
            .size(content.length())
            .inputStream(inputStream)
            .customObjectKey(customKey)
            .build();

        // When
        UploadResult result = client.upload(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObjectKey()).isEqualTo(customKey);
    }

    @Test
    void download_existingFile_shouldReturnInputStream() {
        // Given
        String content = "Download test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        UploadRequest request = UploadRequest.builder()
            .orgId("org-1")
            .operatorId("operator-1")
            .category(FileCategory.ATTACHMENT)
            .originalName("test.txt")
            .contentType("text/plain")
            .size(content.length())
            .inputStream(inputStream)
            .build();
        UploadResult uploadResult = client.upload(request);

        // When
        var result = client.download(uploadResult.getObjectKey());

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void download_nonExistingFile_shouldReturnEmpty() {
        // When
        var result = client.download("non-existing-key");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void delete_existingFile_shouldReturnTrue() {
        // Given
        String content = "Delete test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        UploadRequest request = UploadRequest.builder()
            .orgId("org-1")
            .operatorId("operator-1")
            .category(FileCategory.ATTACHMENT)
            .originalName("test.txt")
            .contentType("text/plain")
            .size(content.length())
            .inputStream(inputStream)
            .build();
        UploadResult uploadResult = client.upload(request);

        // When
        boolean deleted = client.delete(uploadResult.getObjectKey());

        // Then
        assertThat(deleted).isTrue();
        assertThat(client.exists(uploadResult.getObjectKey())).isFalse();
    }

    @Test
    void delete_nonExistingFile_shouldReturnFalse() {
        // When
        boolean deleted = client.delete("non-existing-key");

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void exists_existingFile_shouldReturnTrue() {
        // Given
        String content = "Exists test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        UploadRequest request = UploadRequest.builder()
            .orgId("org-1")
            .operatorId("operator-1")
            .category(FileCategory.ATTACHMENT)
            .originalName("test.txt")
            .contentType("text/plain")
            .size(content.length())
            .inputStream(inputStream)
            .build();
        UploadResult uploadResult = client.upload(request);

        // When
        boolean exists = client.exists(uploadResult.getObjectKey());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void exists_nonExistingFile_shouldReturnFalse() {
        // When
        boolean exists = client.exists("non-existing-key");

        // Then
        assertThat(exists).isFalse();
    }
}
