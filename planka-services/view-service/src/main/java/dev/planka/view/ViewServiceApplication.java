package dev.planka.view;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 视图数据服务启动类
 * <p>
 * 负责组装视图数据，聚合 Schema 定义和卡片数据。
 * 前端通过此服务获取视图数据，而不是直接调用 card-service。
 */
@SpringBootApplication(scanBasePackages = {"dev.planka"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "dev.planka.api")
public class ViewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ViewServiceApplication.class, args);
    }
}
