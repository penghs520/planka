//package cn.planka.schema.controller;
//
//import cn.planka.common.Result;
//import cn.planka.notification.plugin.NotificationChannel;
//import cn.planka.notification.plugin.NotificationChannelDef;
//import cn.planka.schema.service.notification.NotificationPluginService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Collection;
//import java.util.List;
//
///**
// * 通知插件管理 REST API
// *
// * @author Agilean
// * @since 2.0.0
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/notification/plugins")
//@RequiredArgsConstructor
//public class NotificationPluginController {
//
//    private final NotificationPluginService pluginService;
//
//    /**
//     * 获取所有插件列表
//     */
//    @GetMapping
//    public Result<List<NotificationPluginService.PluginInfo>> listPlugins() {
//        return Result.success(pluginService.getAllPlugins());
//    }
//
//    /**
//     * 上传插件
//     */
//    @PostMapping("/upload")
//    public Result<String> uploadPlugin(@RequestParam("file") MultipartFile file) {
//        try {
//            String filename = pluginService.uploadPlugin(file);
//            return Result.success(filename);
//        } catch (Exception e) {
//            log.error("插件上传失败", e);
//            return Result.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 启动插件
//     */
//    @PostMapping("/{pluginId}/start")
//    public Result<Void> startPlugin(@PathVariable String pluginId) {
//        try {
//            pluginService.startPlugin(pluginId);
//            return Result.success();
//        } catch (Exception e) {
//            log.error("插件启动失败: {}", pluginId, e);
//            return Result.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 停止插件
//     */
//    @PostMapping("/{pluginId}/stop")
//    public Result<Void> stopPlugin(@PathVariable String pluginId) {
//        try {
//            pluginService.stopPlugin(pluginId);
//            return Result.success();
//        } catch (Exception e) {
//            log.error("插件停止失败: {}", pluginId, e);
//            return Result.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 删除插件
//     */
//    @DeleteMapping("/{pluginId}")
//    public Result<Void> deletePlugin(@PathVariable String pluginId) {
//        try {
//            pluginService.unloadPlugin(pluginId);
//            return Result.success();
//        } catch (Exception e) {
//            log.error("插件删除失败: {}", pluginId, e);
//            return Result.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 获取所有可用渠道列表
//     */
//    @GetMapping("/channels")
//    public Result<List<ChannelInfo>> listChannels() {
//        Collection<NotificationChannel> channels = pluginService.getAllChannels();
//        List<ChannelInfo> channelInfos = channels.stream()
//                .map(channel -> new ChannelInfo(
//                        channel.channelId(),
//                        channel.name(),
//                        channel.version(),
//                        channel.provider(),
//                        channel.supportsRichContent(),
//                        channel.supportsAttachment(),
//                        channel.def()
//                ))
//                .toList();
//        return Result.success(channelInfos);
//    }
//
//    /**
//     * 获取渠道定义（配置字段）
//     */
//    @GetMapping("/channels/{channelId}/def")
//    public Result<NotificationChannelDef> getChannelDef(@PathVariable String channelId) {
//        Collection<NotificationChannel> channels = pluginService.getAllChannels();
//        NotificationChannel channel = channels.stream()
//                .filter(c -> c.channelId().equals(channelId))
//                .findFirst()
//                .orElse(null);
//
//        if (channel == null) {
//            return Result.error("渠道不存在: " + channelId);
//        }
//
//        return Result.success(channel.def());
//    }
//
//    /**
//     * 渠道信息
//     */
//    public record ChannelInfo(
//            String channelId,
//            String name,
//            String version,
//            String provider,
//            boolean supportsRichContent,
//            boolean supportsAttachment,
//            NotificationChannelDef def
//    ) {}
//}
