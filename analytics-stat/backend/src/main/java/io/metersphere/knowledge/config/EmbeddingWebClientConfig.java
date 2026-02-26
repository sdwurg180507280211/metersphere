package io.metersphere.knowledge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Embedding API 的 WebClient 配置
 * 用于调用通义千问 text-embedding-v4 API 生成向量
 */
@Configuration
public class EmbeddingWebClientConfig {

    @Value("${embedding.api.url}")
    private String apiUrl;

    @Value("${embedding.api.key}")
    private String apiKey;

    @Bean
    public WebClient embeddingWebClient() {
        // 设置最大内存缓冲区为 16MB，防止大批量向量响应超出默认限制
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(apiUrl)
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
