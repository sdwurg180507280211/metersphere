package io.metersphere.simulator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class TestProducer {

    private static final String DEFAULT_CONFIG_PATH = "/opt/metersphere/conf/metersphere.properties";
    private static final String DEFAULT_TOPIC = "topic-requirement-to-metersphere";

    public static void main(String[] args) {
        Properties properties = loadProperties(args);
        String nameServer = getConfig(properties, "requirement.sync.mq.name-server", "ROCKETMQ_NAME_SERVER", "");
        String topic = getConfig(properties, "requirement.sync.mq.topic", "ROCKETMQ_TOPIC", DEFAULT_TOPIC);
        if (nameServer.isBlank()) {
            throw new IllegalArgumentException("RocketMQ NameServer 未配置，请在 /opt/metersphere/conf/metersphere.properties 中配置 requirement.sync.mq.name-server，或设置环境变量 ROCKETMQ_NAME_SERVER");
        }

        RequirementProducer producer = new RequirementProducer(nameServer, topic);

        try {
            producer.start();

            // 创建测试消息
            RequirementSyncMessage message = new RequirementSyncMessage();
            message.setDmpNum("TEST-" + System.currentTimeMillis());
            message.setName1("测试需求-银保微投新产品");
            message.setOperationType("CREATED");
            message.setReqManagerName("张三");
            message.setActName("测试待处理");
            message.setCreateTime(System.currentTimeMillis());
            message.setParentWfinstCode("WF-TEST-001");
            message.setReqFatherClass("功能需求");
            message.setReqSonClass("新增功能");
            message.setSystemName("测试系统");
            message.setUpTime(System.currentTimeMillis() + 86400000L);
            message.setAssigneeName("李四");
            message.setCreatedept("产品部");
            message.setCreateUser1("王五");
            message.setDeptName("产品一部");
            message.setStartUserName("赵六");
            message.setEventTime(System.currentTimeMillis());
            message.setTraceId("trace-" + UUID.randomUUID().toString());

            // 发送消息
            producer.sendMessage(message);

            System.out.println("\n测试消息发送完成！");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            producer.shutdown();
        }
    }

    private static Properties loadProperties(String[] args) {
        String configPath = args.length > 0 && !args[0].isBlank() ? args[0] : DEFAULT_CONFIG_PATH;
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(configPath)) {
            properties.load(inputStream);
            System.out.println("已加载配置文件: " + configPath);
        } catch (IOException e) {
            System.out.println("未加载配置文件: " + configPath + "，将尝试读取环境变量");
        }
        return properties;
    }

    private static String getConfig(Properties properties, String propertyKey, String envKey, String defaultValue) {
        String value = properties.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
