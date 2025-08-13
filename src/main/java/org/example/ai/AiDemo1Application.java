package org.example.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigurationProperties
@MapperScan("org.example.ai.mapper")
@EnableAsync
@EnableScheduling
public class AiDemo1Application {

    public static void main(String[] args) {
        SpringApplication.run(AiDemo1Application.class, args);
    }

    // RestTemplate 会自动装配 MappingJackson2HttpMessageConverter，
    // 其内部同样会使用 Spring 容器中的全局 ObjectMapper（见 JacksonConfig）。
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
