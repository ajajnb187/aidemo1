package org.example.sdspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigurationProperties
public class SdSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdSpringbootApplication.class, args);
    }

    // RestTemplate 会自动装配 MappingJackson2HttpMessageConverter，
    // 其内部同样会使用 Spring 容器中的全局 ObjectMapper（见 JacksonConfig）。
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
