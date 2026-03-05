package io.metersphere.knowledge.controller;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;

public class A {
    public static void main(String[] args) {
        // Configures using the `anthropic.apiKey`, `anthropic.authToken` and `anthropic.baseUrl` system properties
// Or configures using the `ANTHROPIC_API_KEY`, `ANTHROPIC_AUTH_TOKEN` and `ANTHROPIC_BASE_URL` environment variables
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey("aicoding-53023984484a878506ec4082938d6b2e")
                .baseUrl("https://65b94c539de4-vip.aicoding.sh")
                .build();

        //配置多个model，自动匹配能用的模型
        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(1024L)
                .addUserMessage("Hello, Claude")
                .model(Model.CLAUDE_OPUS_4_6)
                .model(Model.CLAUDE_OPUS_4_5)
                .build();

        Message message = client.messages().create(params);

        System.out.println("ok~");
    }
}
