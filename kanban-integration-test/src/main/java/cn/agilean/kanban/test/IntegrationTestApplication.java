package cn.agilean.kanban.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 集成测试启动类
 * <p>
 * 使用 OpenFeign + Nacos 服务发现调用后端服务进行集成测试
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
        "cn.agilean.kanban.api"
})
public class IntegrationTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }
}
