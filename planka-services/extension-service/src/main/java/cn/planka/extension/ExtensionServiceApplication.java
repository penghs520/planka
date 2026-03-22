package cn.planka.extension;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Extension Service 启动类
 * <p>
 * 扩展服务 - 操作历史、附件管理、业务规则、评论等扩展功能
 */
@SpringBootApplication(scanBasePackages = {"cn.planka"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.planka.api")
@MapperScan({"cn.planka.extension.history.mapper", "cn.planka.extension.comment.mapper"})
public class ExtensionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExtensionServiceApplication.class, args);
    }
}
