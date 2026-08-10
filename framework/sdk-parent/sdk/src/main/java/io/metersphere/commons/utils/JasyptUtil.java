package io.metersphere.commons.utils;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * 配置文件密码加密工具
 * <p>
 * 使用方式：
 * 1. 部署时通过环境变量或 JVM 参数传入主密码：
 *    -Djasypt.encryptor.password=你的主密码
 *    或 export JASYPT_ENCRYPTOR_PASSWORD=你的主密码
 * <p>
 * 2. 在配置文件中使用 ENC(加密后密文) 格式存储密码：
 *    spring.datasource.password=ENC(xxxxx)
 * <p>
 * 3. 生成加密密码（命令行或代码调用）：
 *    java -cp sdk.jar io.metersphere.commons.utils.JasyptUtil 加密主密码 明文密码
 */
public class JasyptUtil {

    private static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";
    private static final String IV_GENERATOR = "org.jasypt.iv.RandomIvGenerator";

    /**
     * 加密
     *
     * @param masterPassword 主密码（部署时传入）
     * @param plainText     明文
     * @return ENC(密文)
     */
    public static String encrypt(String masterPassword, String plainText) {
        PooledPBEStringEncryptor encryptor = buildEncryptor(masterPassword);
        return "ENC(" + encryptor.encrypt(plainText) + ")";
    }

    /**
     * 解密
     *
     * @param masterPassword 主密码
     * @param encryptedText  密文（不含 ENC() 包裹）
     * @return 明文
     */
    public static String decrypt(String masterPassword, String encryptedText) {
        PooledPBEStringEncryptor encryptor = buildEncryptor(masterPassword);
        return encryptor.decrypt(encryptedText);
    }

    private static PooledPBEStringEncryptor buildEncryptor(String masterPassword) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(masterPassword);
        config.setAlgorithm(ALGORITHM);
        config.setIvGeneratorClassName(IV_GENERATOR);
        config.setPoolSize(1);
        config.setProviderName(null);
        config.setKeyObtentionIterations(1000);
        encryptor.setConfig(config);
        return encryptor;
    }

//    public static void main(String[] args) {
////        if (args.length < 2) {
////            System.out.println("用法: java JasyptUtil <主密码> <明文密码>");
////            System.out.println("示例: java JasyptUtil metersphere Password123!");
////            return;
////        }
//        String encrypted = encrypt("metersphere", "Password123@minio");
//        System.out.println("加密结果: " + encrypted);
//        System.out.println("配置文件中使用: spring.datasource.password=" + encrypted);
//    }
}
