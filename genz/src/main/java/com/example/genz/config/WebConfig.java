package com.example.genz.config;

import com.example.genz.security.interceptor.StepUpAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;
    private final StepUpAuthenticationInterceptor stepUpAuthenticationInterceptor;

    public WebConfig(
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,https://localhost:*}")
            String[] allowedOriginPatterns,
            StepUpAuthenticationInterceptor stepUpAuthenticationInterceptor
    ) {
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.stepUpAuthenticationInterceptor = stepUpAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(stepUpAuthenticationInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Step-Up-Token")
                .allowCredentials(true);
    }
}
