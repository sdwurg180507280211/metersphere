# 产品概述

MeterSphere 是一站式开源持续测试平台，提供企业级的全面测试能力。

## 核心功能

- **测试跟踪**：测试用例管理、测试计划执行、缺陷跟踪，无缝对接主流项目管理平台
- **接口测试**：支持多协议（HTTP、WebSocket、Dubbo、TCP）的 REST API 测试、场景编排和 Mock 服务
- **性能测试**：兼容 JMeter 的分布式压测，实时监控和完善的测试报告
- **UI 测试**：基于 Selenium 的浏览器自动化，低代码测试脚本创建

## 架构设计

MeterSphere 采用微服务架构，包含 9 个独立服务：

- **框架层**：Gateway（API 网关）、Eureka（服务注册中心）、SDK（共享类库）
- **业务层**：接口测试、性能测试、测试跟踪、项目管理、系统设置、报告统计、工作台

## 技术基础

- 后端：Spring Boot 3.2.12 + Spring Cloud + Java 17
- 前端：Vue.js 2.7 + qiankun 微前端架构
- 测试引擎：JMeter 5.5（性能测试）、Selenium 4.10（UI 测试）
- 基础设施：MySQL 8.0、Kafka 3.6、Redis、MinIO

## 版本信息

当前 LTS 版本：v2.10-lts（2023年5月发布）
- 每月发布小版本，持续进行 Bug 修复和部分功能优化
- 企业扩展功能通过 xpack 插件提供
