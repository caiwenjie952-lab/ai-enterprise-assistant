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

## Day 18 完成内容

- 新增 `context.ChatContext`，统一承载本轮聊天的 `conversationId`、当前用户消息、模型上下文消息、会话摘要和最近历史条数。
- 新增 `context.ChatContextBuilder`，集中负责生成或复用会话 ID、读取摘要、读取最近历史、分配用户消息序号，并组装模型调用上下文。
- 新增 `lifecycle.ConversationLifecycleResult` 和 `lifecycle.ConversationLifecycleService`，抽象模型回复后的会话生命周期处理结果与入口。
- 新增 `lifecycle.impl.ConversationLifecycleServiceImpl`，集中负责 assistant 消息序号分配、保存 user/assistant 消息、判断并更新摘要、更新摘要位置以及裁剪内存消息。
- 重构 `ChatServiceImpl`，只保留同步聊天编排：构建上下文、调用模型、执行会话生命周期、返回 `ChatResponse`。
- 重构 `ChatStreamServiceImpl`，只保留 SSE 相关逻辑和流式模型调用，流式回复完成后交给生命周期服务处理会话记忆。

## Day 18 职责划分

- `ChatContextBuilder`：负责“模型调用前”的上下文准备。
- `ConversationLifecycleService`：负责“模型回复后”的会话状态更新。
- `ChatServiceImpl`：负责普通 HTTP 聊天接口编排。
- `ChatStreamServiceImpl`：负责 SSE 流式聊天接口编排。

接口路径保持不变：

- `GET /chat/test`
- `POST /chat`
- `POST /chat/stream`
- `GET /conversation/{conversationId}`
- `DELETE /conversation/{conversationId}`

## Day 19 完成内容

- 新增 `ChatModelClient.generateTitle(String userMessage)`，统一抽象会话标题生成能力。
- `MockChatModelClient` 根据用户首条消息截取前 12 个字符作为标题，空消息返回“新会话”。
- `DeepSeekChatModelClient` 使用非流式 `/chat/completions` 生成 5 到 15 个中文字符标题，失败时回退到用户消息前 12 个字符，且不影响正常聊天。
- `ConversationMemoryService` 增加标题读写能力：`getTitle`、`updateTitle`、`hasTitle`。
- `InMemoryConversationMemoryServiceImpl` 使用 `titleMemory` 保存会话标题，并在清空会话时同步清理。
- `ConversationLifecycleServiceImpl` 在保存 user/assistant 消息后，为首次会话自动生成并保存标题。
- `ChatResponse`、`ConversationDetailResponse` 增加 `title` 字段。
- `POST /chat/stream` 在流式完成后新增 `conversation` SSE 事件，返回 `conversationId` 和 `title`，原有 `start/message/summary/done/error` 事件保持不变。
- 前端页面增加“当前标题”展示，并支持读取 `conversation` 事件和会话详情中的标题。
