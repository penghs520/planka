package cn.planka.comment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Comment Service 启动类 — 评论与建议
 */
@SpringBootApplication(scanBasePackages = {"cn.planka"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.planka.api")
@MapperScan({"cn.planka.comment.mapper"})
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }
}
