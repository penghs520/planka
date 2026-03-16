package cn.planka.notification.plugin;

import org.pf4j.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HybridPluginManagerTest {

    @Mock
    private PluginManager pf4jManager;

    @Mock
    private NotificationChannel springChannel;

    @Mock
    private NotificationChannelPlugin pf4jChannel;

    private HybridPluginManager hybridPluginManager;

    @BeforeEach
    void setUp() {
        when(springChannel.channelId()).thenReturn("builtin");
        when(springChannel.name()).thenReturn("系统通知");

        hybridPluginManager = new HybridPluginManager(
                pf4jManager,
                List.of(springChannel)
        );
    }

    @Test
    void getChannel_shouldReturnSpringChannel_whenPf4jChannelNotFound() {
        // Given
        when(pf4jManager.getExtensions(NotificationChannelPlugin.class))
                .thenReturn(Collections.emptyList());

        // When
        Optional<NotificationChannel> result = hybridPluginManager.getChannel("builtin");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(springChannel);
    }

    @Test
    void getChannel_shouldReturnPf4jChannel_whenPf4jChannelFound() {
        // Given
        when(pf4jChannel.channelId()).thenReturn("email");
        when(pf4jManager.getExtensions(NotificationChannelPlugin.class))
                .thenReturn(List.of(pf4jChannel));

        // When
        Optional<NotificationChannel> result = hybridPluginManager.getChannel("email");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(pf4jChannel);
    }

    @Test
    void getChannel_shouldReturnEmpty_whenChannelNotFound() {
        // Given
        when(pf4jManager.getExtensions(NotificationChannelPlugin.class))
                .thenReturn(Collections.emptyList());

        // When
        Optional<NotificationChannel> result = hybridPluginManager.getChannel("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getAllChannels_shouldReturnAllChannels() {
        // Given
        when(pf4jChannel.channelId()).thenReturn("email");
        when(pf4jManager.getExtensions(NotificationChannelPlugin.class))
                .thenReturn(List.of(pf4jChannel));

        // When
        var result = hybridPluginManager.getAllChannels();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(springChannel, pf4jChannel);
    }

    @Test
    void getAllChannels_shouldOverrideSpringChannelWithPf4jChannel_whenSameChannelId() {
        // Given
        when(pf4jChannel.channelId()).thenReturn("builtin");
        when(pf4jManager.getExtensions(NotificationChannelPlugin.class))
                .thenReturn(List.of(pf4jChannel));

        // When
        var result = hybridPluginManager.getAllChannels();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(pf4jChannel);
        assertThat(result).doesNotContain(springChannel);
    }
}
