package cn.planka.schema;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Schema 服务启动类
 */
@SpringBootApplication(scanBasePackages = {"cn.planka"})
@MapperScan("cn.planka.schema.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.planka.api")
public class SchemaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaServiceApplication.class, args);
    }
}
