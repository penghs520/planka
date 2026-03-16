//package cn.planka.schema.service.notification;
//
//import cn.planka.notification.plugin.HybridPluginManager;
//import cn.planka.notification.plugin.NotificationChannel;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockMultipartFile;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class NotificationPluginServiceTest {
//
//    @Mock
//    private HybridPluginManager pluginManager;
//
//    @Mock
//    private NotificationChannel channel;
//
//    private NotificationPluginService pluginService;
//
//    @BeforeEach
//    void setUp() {
//        pluginService = new NotificationPluginService(pluginManager);
//    }
//
//    @Test
//    void getAllPlugins_shouldReturnPluginList() {
//        // Given
//        HybridPluginManager.PluginInfo pluginInfo = HybridPluginManager.PluginInfo.builder()
//                .pluginId("email")
//                .pluginState("STARTED")
//                .version("1.0.0")
//                .provider("Planka")
//                .build();
//        when(pluginManager.getAllPlugins()).thenReturn(List.of(pluginInfo));
//
//        // When
//        List<NotificationPluginService.PluginInfo> result = pluginService.getAllPlugins();
//
//        // Then
//        assertThat(result).hasSize(1);
//        assertThat(result.get(0).pluginId()).isEqualTo("email");
//        assertThat(result.get(0).pluginState()).isEqualTo("STARTED");
//    }
//
//    @Test
//    void getAllChannels_shouldReturnChannelList() {
//        // Given
//        when(pluginManager.getAllChannels()).thenReturn(List.of(channel));
//
//        // When
//        var result = pluginService.getAllChannels();
//
//        // Then
//        assertThat(result).hasSize(1);
//        assertThat(result).contains(channel);
//    }
//
//    @Test
//    void uploadPlugin_shouldThrowException_whenFileIsEmpty() {
//        // Given
//        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
//
//        // When & Then
//        assertThatThrownBy(() -> pluginService.uploadPlugin(emptyFile))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("文件不能为空");
//    }
//
//    @Test
//    void uploadPlugin_shouldThrowException_whenFileIsNotJar() {
//        // Given
//        MockMultipartFile txtFile = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "content".getBytes()
//        );
//
//        // When & Then
//        assertThatThrownBy(() -> pluginService.uploadPlugin(txtFile))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("只支持 JAR 文件");
//    }
//
//    @Test
//    void uploadPlugin_shouldThrowException_whenFileSizeExceedsLimit() {
//        // Given
//        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
//        MockMultipartFile largeFile = new MockMultipartFile(
//                "file",
//                "test.jar",
//                "application/java-archive",
//                largeContent
//        );
//
//        // When & Then
//        assertThatThrownBy(() -> pluginService.uploadPlugin(largeFile))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("文件大小超过限制（50MB）");
//    }
//
//    @Test
//    void startPlugin_shouldCallPluginManager() {
//        // Given
//        String pluginId = "email";
//
//        // When
//        pluginService.startPlugin(pluginId);
//
//        // Then
//        verify(pluginManager).startPlugin(pluginId);
//    }
//
//    @Test
//    void stopPlugin_shouldCallPluginManager() {
//        // Given
//        String pluginId = "email";
//
//        // When
//        pluginService.stopPlugin(pluginId);
//
//        // Then
//        verify(pluginManager).stopPlugin(pluginId);
//    }
//
//    @Test
//    void unloadPlugin_shouldCallPluginManager() {
//        // Given
//        String pluginId = "email";
//
//        // When
//        pluginService.unloadPlugin(pluginId);
//
//        // Then
//        verify(pluginManager).unloadPlugin(pluginId);
//    }
//}
