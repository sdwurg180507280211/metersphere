package io.metersphere.knowledge.config;

import co.elastic.clients.transport.ElasticsearchTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * Elasticsearch 客户端配置类
 * 从 PaiSmart 迁移，改包名为 io.metersphere.knowledge.config
 * 连接参数通过 application.properties 注入
 */
@Configuration
public class EsConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.scheme:https}")
    private String scheme;

    @Value("${elasticsearch.username:elastic}")
    private String username;

    @Value("${elasticsearch.password:changeme}")
    private String password;

    @Value("${elasticsearch.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${elasticsearch.socket-timeout-ms:120000}")
    private int socketTimeoutMs;

    @Value("${elasticsearch.connection-request-timeout-ms:10000}")
    private int connectionRequestTimeoutMs;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 创建低级 REST 客户端
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));
        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(connectTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .setConnectionRequestTimeout(connectionRequestTimeoutMs));

        // 设置基本认证和 TLS（开发环境忽略证书验证）
        if (username != null && !username.isEmpty()) {
            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                try {
                    SSLContext sslContext = SSLContexts.custom()
                            .loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true)
                            .build();
                    httpClientBuilder.setSSLContext(sslContext);
                    httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                } catch (Exception e) {
                    // 忽略 SSL 配置异常（仅限开发环境）
                }
                return httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
            });
        }

        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
