package cn.planka;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * History Service 启动类 — 卡片操作历史
 */
@SpringBootApplication(scanBasePackages = {"cn.planka"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.planka.api")
@MapperScan({"cn.planka.history.mapper"})
public class HistoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HistoryServiceApplication.class, args);
    }
}
