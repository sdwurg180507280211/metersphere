package io.metersphere.eureka.config;

import org.jasypt.encryption.StringEncryptor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redisson 配置，支持 redisson.yml 中密码使用 ENC() 加密
 */
@Configuration
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);
    private static final Pattern ENC_PATTERN = Pattern.compile("ENC\\(([^)]+)\\)");

    @Value("${spring.redis.redisson.file:}")
    private String redissonFile;

    private final ObjectProvider<StringEncryptor> encryptorProvider;

    public RedissonConfig(ObjectProvider<StringEncryptor> encryptorProvider) {
        this.encryptorProvider = encryptorProvider;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "spring.redis.redisson.file")
    public RedissonClient redissonClient() throws Exception {
        String path = redissonFile.replace("file:", "");
        File file = ResourceUtils.getFile(path);
        String yamlContent = new String(Files.readAllBytes(file.toPath()));
        yamlContent = decryptYaml(yamlContent);
        Config config = Config.fromYAML(yamlContent);
        return Redisson.create(config);
    }

    private String decryptYaml(String yaml) {
        StringEncryptor encryptor = encryptorProvider.getIfAvailable();
        if (encryptor == null) {
            log.warn("StringEncryptor 未找到（未设置 jasypt.encryptor.password），redisson.yml 中的 ENC() 密码将不会被解密");
            return yaml;
        }
        Matcher matcher = ENC_PATTERN.matcher(yaml);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String encrypted = matcher.group(1);
            String decrypted;
            try {
                decrypted = encryptor.decrypt(encrypted);
                log.info("解密成功: {} -> {}", fullMatch, decrypted.replaceAll(".", "*"));
            } catch (Exception e) {
                log.error("解密失败: {}, 错误: {}", fullMatch, e.getMessage());
                matcher.appendReplacement(sb, Matcher.quoteReplacement(fullMatch));
                continue;
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(decrypted));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
