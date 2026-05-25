package io.metersphere.simulator;

import java.util.UUID;

public class TestProducer {
    public static void main(String[] args) {
        String nameServer = "192.168.8.101:9876";
        String topic = "topic-requirement-to-metersphere";

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
}
