package com.example.genz.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(PlasGateProperties.class) 
public class PlasGateConfig {

    @Bean
    public RestClient plasGateRestClient(
        RestClient.Builder builder,
        PlasGateProperties properties
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        String baseUrl = properties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://cloudapi.plasgate.com";
        }
		return builder
            .requestFactory(factory)
            .baseUrl(baseUrl)    
            .build();

    }
}
