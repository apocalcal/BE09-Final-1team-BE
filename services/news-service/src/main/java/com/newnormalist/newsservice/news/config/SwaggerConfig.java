package com.newnormalist.newsservice.news.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("뉴스 플랫폼 API")
                        .version("1.0.0")
                        .description("NewsController 엔드포인트 명세서"));
    }
}
