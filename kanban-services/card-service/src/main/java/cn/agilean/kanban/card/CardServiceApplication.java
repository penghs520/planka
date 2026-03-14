package cn.agilean.kanban.card;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"cn.agilean.kanban"})
@MapperScan("cn.agilean.kanban.card.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.agilean.kanban.api")
public class CardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardServiceApplication.class);
    }
}
