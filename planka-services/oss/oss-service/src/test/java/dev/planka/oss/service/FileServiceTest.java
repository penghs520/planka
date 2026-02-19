package dev.planka.oss.service;

import dev.planka.oss.api.dto.FileDTO;
import dev.planka.oss.config.OssPluginRegistry;
import dev.planka.oss.config.OssProperties;
import dev.planka.oss.entity.FileMeta;
import dev.planka.oss.repository.FileMetaRepository;
import dev.planka.oss.plugin.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private OssPluginRegistry pluginRegistry;

    @Mock
    private OssProperties ossProperties;

    @Mock
    private FileMetaRepository fileMetaRepository;

    @Mock
    private OssPlugin ossPlugin;

    @Mock
    private OssClient ossClient;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(pluginRegistry, ossProperties, fileMetaRepository);
    }

    @Test
    void upload_shouldSaveFileMetaAndReturnDTO() {
        // Given
        String content = "test content";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            content.getBytes(StandardCharsets.UTF_8)
        );
        String orgId = "org-1";
        String operatorId = "member-1";
        FileCategory category = FileCategory.ATTACHMENT;

        when(ossProperties.getActivePlugin()).thenReturn("local");
        when(pluginRegistry.getPlugin("local")).thenReturn(Optional.of(ossPlugin));
        when(ossPlugin.getClient()).thenReturn(ossClient);
        when(ossPlugin.pluginId()).thenReturn("local");
        when(ossClient.upload(any(UploadRequest.class))).thenReturn(
            UploadResult.success("org-1/attachments/uuid.txt", "/files/org-1/attachments/uuid.txt", content.length(), "text/plain")
        );
        when(fileMetaRepository.insert(any(FileMeta.class))).thenReturn(1);

        // When
        FileDTO result = fileService.upload(file, orgId, operatorId, category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrgId()).isEqualTo(orgId);
        assertThat(result.getOperatorId()).isEqualTo(operatorId);
        assertThat(result.getCategory()).isEqualTo(category);
        assertThat(result.getOriginalName()).isEqualTo("test.txt");
        assertThat(result.getContentType()).isEqualTo("text/plain");

        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        verify(fileMetaRepository).insert(fileMetaCaptor.capture());
        FileMeta savedMeta = fileMetaCaptor.getValue();
        assertThat(savedMeta.getId()).isNotBlank();
        assertThat(savedMeta.getStoragePlugin()).isEqualTo("local");
    }

    @Test
    void getFile_existingFile_shouldReturnDTO() {
        // Given
        String fileId = "file-1";
        FileMeta fileMeta = new FileMeta();
        fileMeta.setId(fileId);
        fileMeta.setOrgId("org-1");
        fileMeta.setOperatorId("member-1");
        fileMeta.setCategory(FileCategory.ATTACHMENT);
        fileMeta.setOriginalName("test.txt");
        fileMeta.setUrl("/files/test.txt");
        fileMeta.setSize(100L);
        fileMeta.setContentType("text/plain");

        when(fileMetaRepository.selectById(fileId)).thenReturn(fileMeta);

        // When
        Optional<FileDTO> result = fileService.getFile(fileId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(fileId);
    }

    @Test
    void getFile_nonExistingFile_shouldReturnEmpty() {
        // Given
        String fileId = "non-existing";
        when(fileMetaRepository.selectById(fileId)).thenReturn(null);

        // When
        Optional<FileDTO> result = fileService.getFile(fileId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void download_existingFile_shouldReturnInputStream() {
        // Given
        String fileId = "file-1";
        FileMeta fileMeta = new FileMeta();
        fileMeta.setId(fileId);
        fileMeta.setObjectKey("org-1/attachments/test.txt");
        fileMeta.setStoragePlugin("local");

        InputStream expectedStream = new ByteArrayInputStream("content".getBytes());

        when(fileMetaRepository.selectById(fileId)).thenReturn(fileMeta);
        when(pluginRegistry.getPlugin("local")).thenReturn(Optional.of(ossPlugin));
        when(ossPlugin.getClient()).thenReturn(ossClient);
        when(ossClient.download("org-1/attachments/test.txt")).thenReturn(Optional.of(expectedStream));

        // When
        Optional<InputStream> result = fileService.download(fileId);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void delete_existingFile_shouldDeleteAndReturnTrue() {
        // Given
        String fileId = "file-1";
        FileMeta fileMeta = new FileMeta();
        fileMeta.setId(fileId);
        fileMeta.setObjectKey("org-1/attachments/test.txt");
        fileMeta.setStoragePlugin("local");

        when(fileMetaRepository.selectById(fileId)).thenReturn(fileMeta);
        when(pluginRegistry.getPlugin("local")).thenReturn(Optional.of(ossPlugin));
        when(ossPlugin.getClient()).thenReturn(ossClient);
        when(ossClient.delete("org-1/attachments/test.txt")).thenReturn(true);
        when(fileMetaRepository.deleteById(fileId)).thenReturn(1);

        // When
        boolean result = fileService.delete(fileId);

        // Then
        assertThat(result).isTrue();
        verify(fileMetaRepository).deleteById(fileId);
    }

    @Test
    void getDownloadUrl_withPresignedSupport_shouldReturnPresignedUrl() {
        // Given
        String fileId = "file-1";
        FileMeta fileMeta = new FileMeta();
        fileMeta.setId(fileId);
        fileMeta.setObjectKey("org-1/attachments/test.txt");
        fileMeta.setStoragePlugin("minio");

        when(fileMetaRepository.selectById(fileId)).thenReturn(fileMeta);
        when(pluginRegistry.getPlugin("minio")).thenReturn(Optional.of(ossPlugin));
        when(ossPlugin.supportsPresignedUrl()).thenReturn(true);
        when(ossPlugin.getClient()).thenReturn(ossClient);
        when(ossClient.generatePresignedDownloadUrl(eq("org-1/attachments/test.txt"), any(Duration.class)))
            .thenReturn(Optional.of("https://minio/presigned-url"));

        // When
        Optional<String> result = fileService.getDownloadUrl(fileId, Duration.ofHours(1));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("https://minio/presigned-url");
    }

    @Test
    void getDownloadUrl_withoutPresignedSupport_shouldReturnStaticUrl() {
        // Given
        String fileId = "file-1";
        FileMeta fileMeta = new FileMeta();
        fileMeta.setId(fileId);
        fileMeta.setObjectKey("org-1/attachments/test.txt");
        fileMeta.setUrl("/files/org-1/attachments/test.txt");
        fileMeta.setStoragePlugin("local");

        when(fileMetaRepository.selectById(fileId)).thenReturn(fileMeta);
        when(pluginRegistry.getPlugin("local")).thenReturn(Optional.of(ossPlugin));
        when(ossPlugin.supportsPresignedUrl()).thenReturn(false);

        // When
        Optional<String> result = fileService.getDownloadUrl(fileId, Duration.ofHours(1));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("/files/org-1/attachments/test.txt");
    }
}
