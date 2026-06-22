# 10 满意度评价入口

## 目标

让游客能对 AI 回答进行满意度评价，并将评价数据用于交互日志和数据大屏满意度趋势。

## 当前情况

后端交互日志表包含满意度相关字段，但前端没有评价入口。当前 `AiChatService` 在对话完成后异步写入日志，但前端无法对某条回答追加评价。

## 涉及文件

- `src/main/resources/static/index.html`
- `src/main/java/com/aicust/model/InteractionLog.java`
- `src/main/java/com/aicust/repository/InteractionLogRepository.java`
- 可新增：`src/main/java/com/aicust/controller/FeedbackController.java`
- 可新增：`src/main/java/com/aicust/dto/FeedbackRequest.java`

## 实施难点

当前聊天接口是流式返回，前端可能不知道本次回答对应哪条 `interaction_log` 记录 ID。

因此需要设计评价关联方式。

## 推荐实现方案

### 短期方案：按用户最近一条日志更新

新增接口：

```text
POST /api/feedback/latest
```

请求：

```json
{
  "rating": 5,
  "comment": "回答很详细"
}
```

后端根据当前登录用户找到最近一条 interaction_log，更新满意度。

优点：实现快。

缺点：如果用户连续快速发送多条消息，可能关联不准。

### 长期方案：聊天 SSE 返回 interactionId

在聊天完成写入日志后，将日志 ID 返回给前端。由于当前日志在 `doOnComplete` 后写入，SSE 结束时再返回 ID 会复杂一些。

长期可以改成：

1. 对话开始创建 pending log。
2. SSE 开始时返回 `interactionId`。
3. 对话完成后更新回答内容。
4. 前端评价时按 ID 更新。

## 短期实施步骤

### 步骤 1：新增 FeedbackRequest

字段：

- `rating`：1-5
- `comment`：可选

### 步骤 2：Repository 增加查询最近日志

例如：

```java
Optional<InteractionLog> findTopByUserIdOrderByCreatedAtDesc(Long userId);
```

字段名以实体实际时间字段为准。

### 步骤 3：新增 FeedbackController

```java
@PostMapping("/api/feedback/latest")
public Map<String, Object> feedbackLatest(@RequestBody FeedbackRequest req) { ... }
```

从 SecurityContext 获取当前用户 ID。

### 步骤 4：前端增加评价按钮

每条 AI 回复结束后显示：

- 👍 满意
- 😐 一般
- 👎 不满意

可映射为：

- 满意：5
- 一般：3
- 不满意：1

点击后调用 `/api/feedback/latest`。

### 步骤 5：防重复评价

前端每条消息增加：

```js
feedbackSent: false
```

提交成功后禁用按钮。

## 验收标准

1. AI 回复完成后出现满意度按钮。
2. 点击后能成功调用后端接口。
3. 数据库最近一条交互日志满意度字段被更新。
4. 数据大屏满意度趋势能统计到评价结果。
5. 重复点击不会重复提交。

## 风险点

- “最近一条日志”方案在并发场景不够严谨，但适合先完成演示。
- 如果 `interaction_log` 当前没有 comment 字段，需要只写 rating，或新增字段。
- 数据大屏如果当前使用情感分而不是满意度，需要同步调整统计逻辑。
