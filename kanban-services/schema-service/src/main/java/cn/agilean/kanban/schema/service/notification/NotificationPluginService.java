//package cn.agilean.kanban.schema.service.notification;
//
//import cn.agilean.kanban.notification.plugin.HybridPluginManager;
//import cn.agilean.kanban.notification.plugin.NotificationChannel;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.Collection;
//import java.util.List;
//import java.util.UUID;
//
///**
// * 通知插件管理服务
// *
// * @author Agilean
// * @since 2.0.0
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class NotificationPluginService {
//
//    private final HybridPluginManager pluginManager;
//    private static final String PLUGIN_UPLOAD_DIR = "./plugins";
//    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
//
//    /**
//     * 获取所有插件信息
//     */
//    public List<PluginInfo> getAllPlugins() {
//        return pluginManager.getAllPlugins().stream()
//                .map(info -> new PluginInfo(
//                        info.getPluginId(),
//                        info.getPluginState(),
//                        info.getVersion(),
//                        info.getProvider()
//                ))
//                .toList();
//    }
//
//    /**
//     * 获取所有可用渠道
//     */
//    public Collection<NotificationChannel> getAllChannels() {
//        return pluginManager.getAllChannels();
//    }
//
//    /**
//     * 上传并加载插件
//     */
//    public String uploadPlugin(MultipartFile file) throws IOException {
//        // 验证文件
//        if (file.isEmpty()) {
//            throw new IllegalArgumentException("文件不能为空");
//        }
//
//        if (file.getSize() > MAX_FILE_SIZE) {
//            throw new IllegalArgumentException("文件大小超过限制（50MB）");
//        }
//
//        String originalFilename = file.getOriginalFilename();
//        if (originalFilename == null || !originalFilename.endsWith(".jar")) {
//            throw new IllegalArgumentException("只支持 JAR 文件");
//        }
//
//        // 创建插件目录
//        Path pluginDir = Paths.get(PLUGIN_UPLOAD_DIR);
//        if (!Files.exists(pluginDir)) {
//            Files.createDirectories(pluginDir);
//        }
//
//        // 保存文件
//        String filename = UUID.randomUUID().toString() + "_" + originalFilename;
//        Path targetPath = pluginDir.resolve(filename);
//        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
//
//        log.info("插件文件已保存: {}", targetPath);
//
//        // 加载插件
//        try {
//            pluginManager.loadPlugin(targetPath);
//            log.info("插件加载成功: {}", filename);
//            return filename;
//        } catch (Exception e) {
//            // 加载失败，删除文件
//            Files.deleteIfExists(targetPath);
//            log.error("插件加载失败: {}", filename, e);
//            throw new RuntimeException("插件加载失败: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 启动插件
//     */
//    public void startPlugin(String pluginId) {
//        pluginManager.startPlugin(pluginId);
//    }
//
//    /**
//     * 停止插件
//     */
//    public void stopPlugin(String pluginId) {
//        pluginManager.stopPlugin(pluginId);
//    }
//
//    /**
//     * 卸载插件
//     */
//    public void unloadPlugin(String pluginId) {
//        pluginManager.unloadPlugin(pluginId);
//    }
//
//    /**
//     * 插件信息
//     */
//    public record PluginInfo(
//            String pluginId,
//            String pluginState,
//            String version,
//            String provider
//    ) {}
//}
