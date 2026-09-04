package com.example.genz.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NextGoal API Documentation")
                        .version("1.0")
                        .description("Backend APIs for the NextGoal project. Integrated with GKE."));
    }
}