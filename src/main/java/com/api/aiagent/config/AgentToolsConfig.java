package com.api.aiagent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AgentToolProperties.class)
public class AgentToolsConfig {

    @Bean
    public HttpClient agentToolsHttpClient(AgentToolProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getHttpTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
