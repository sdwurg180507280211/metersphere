package io.metersphere.knowledge.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 知识问答 Claude SDK 客户端配置
 */
@Configuration
public class KnowledgeChatAnthropicClientConfig {

    @Value("${chat.api.url:}")
    private String apiUrl;

    @Value("${chat.api.key:}")
    private String apiKey;

    @Value("${chat.api.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    public AnthropicClient knowledgeChatAnthropicClient() {
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder();

        if (StringUtils.isNotBlank(apiKey)) {
            builder.apiKey(apiKey.trim());
        }
        if (StringUtils.isNotBlank(apiUrl)) {
            builder.baseUrl(apiUrl.trim());
        }
        if (timeoutSeconds > 0) {
            builder.timeout(Duration.ofSeconds(timeoutSeconds));
        }

        return builder.build();
    }
}
