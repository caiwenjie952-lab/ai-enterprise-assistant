# ai-enterprise-assistant

## 项目定位

本项目是一个 Java 企业级 AI 应用工程实践项目。

目标是基于 Spring Boot / Spring AI / RAG / Agent / 私有化模型部署，构建一个可用于企业知识库问答、业务接口调用、模型切换和工程化管理的 AI 助手系统。

## 技术方向

- Java 21
- Spring Boot 3.5.x
- Spring AI
- DeepSeek / Qwen / 豆包 / 混元
- RAG 知识库
- Agent 工具调用
- 向量数据库
- 私有化模型部署
- 权限、日志、审计、监控

## Day 1 目标

- [x] 创建 Spring Boot 项目
- [x] 设计基础包结构
- [x] 新增统一返回对象 Result
- [x] 新增 ChatController
- [x] 新增 ChatService
- [x] 跑通 /chat/test 接口

## 当前接口

### GET /chat/test

用于验证项目是否启动成功。

当前 VSCode 终端临时切到 JDK 21
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version

## Day 21：MySQL 持久化

- SQL：`src/main/resources/sql/day21.sql`
- 数据库配置通过 `DB_USERNAME`、`DB_PASSWORD` 环境变量注入，默认用户名为 `root`，密码为空。
- 普通聊天在模型成功返回后，将本轮 user/assistant 消息同时写入 Memory 和 MySQL，并更新 `current_message_index`。
- Memory 负责当前进程内的上下文；MySQL 负责保存完整历史消息和会话元数据，数据库历史不会因 Memory 裁剪而删除。
- 当前真实代码版本尚未实现 SSE、title 自动生成、summary、Memory 裁剪及会话管理接口，因此 Day21 未虚构这些生命周期逻辑。
- Day21 只完成写库，尚未完成服务重启后的会话恢复；旧 conversationId 重启后继续使用时，messageIndex 可能需要从数据库恢复，此能力将在 Day22 实现。
