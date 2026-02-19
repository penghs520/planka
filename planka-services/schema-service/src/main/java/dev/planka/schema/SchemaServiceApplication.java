package dev.planka.schema;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Schema 服务启动类
 */
@SpringBootApplication(scanBasePackages = {"dev.planka"})
@MapperScan("dev.planka.schema.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "dev.planka.api")
public class SchemaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaServiceApplication.class, args);
    }
}
