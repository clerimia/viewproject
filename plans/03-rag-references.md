# 03 RAG 参考资料前端展示闭环

## 目标

让用户不仅看到 AI 回答，还能展开查看 RAG 检索到的原文出处，形成“检索 → 生成 → 可追溯展示”的核心闭环。

## 当前情况

后端已经在 `AiChatService` 中检索 RAG 文档，并将命中结果拼入 system prompt。但这些命中结果只给 LLM 使用，没有返回给前端展示。

当前 `/api/chat` 返回 `Flux<String>`，前端只按普通 `data:` 文本流处理。

## 涉及文件

- `src/main/java/com/aicust/service/AiChatService.java`
- `src/main/java/com/aicust/controller/ChatController.java`
- `src/main/resources/static/index.html`

## 推荐实现方案

使用 SSE 事件区分参考资料和正文消息。

### 后端事件格式

在回答 token 流开始前，先发送：

```text
event: references
data: [{"title":"...","category":"...","score":0.86,"text":"..."}]

```

正式回答继续发送：

```text
event: message
data: 这里是模型输出片段

```

### 步骤 1：定义参考资料 DTO 或 Map

可以先不新增 DTO，直接将 `SearchHit` 转为 `Map`：

```java
Map.of(
  "title", hit.title(),
  "category", hit.category(),
  "score", hit.fusedScore(),
  "text", hit.text()
)
```

### 步骤 2：调整 `AiChatService.streamChat()` 返回内容

当前方法：

```java
public Flux<String> streamChat(Long userId, String prompt, String mode)
```

可以保持返回 `Flux<String>`，但字符串内容改成完整 SSE 片段。

构造：

```java
Flux<String> referencesEvent = Flux.just("event: references\ndata: " + json + "\n\n");
Flux<String> messageFlux = chatClient...content().map(chunk -> "event: message\ndata: " + escape(chunk) + "\n\n");
return Flux.concat(referencesEvent, messageFlux)...;
```

注意：Spring `produces = TEXT_EVENT_STREAM_VALUE` 可能会自动包装 `data:`。如果当前前端已经收到 `data:` 行，需要实际测试，避免双重 `data:`。

更稳妥方案是返回 JSON 行：

```json
{"type":"references","items":[...]}
{"type":"message","content":"..."}
```

前端按 JSON 解析。

### 步骤 3：前端消息结构增加 references

```js
const aiMsg = {
  role: 'assistant',
  content: '',
  references: [],
  steps: [],
  isTyping: true
};
```

### 步骤 4：前端解析 references 事件

当前前端只处理：

```js
if (line.startsWith('data:')) {
  const chunk = line.substring(5);
  aiMsg.content += chunk;
}
```

需要增强为：

- 识别 `event: references`
- 下一行 `data:` 作为 JSON 解析
- 写入 `aiMsg.references`
- `event: message` 的 `data:` 才拼入正文

如果采用 JSON 行，则判断：

```js
const payload = JSON.parse(chunk);
if (payload.type === 'references') aiMsg.references = payload.items;
if (payload.type === 'message') aiMsg.content += payload.content;
```

### 步骤 5：增加折叠区 UI

在 AI 回复气泡下方增加：

```html
<details v-if="msg.references && msg.references.length" class="mt-2 text-xs">
  <summary class="cursor-pointer text-emerald-700">参考资料 {{ msg.references.length }} 条</summary>
  <div v-for="ref in msg.references" class="mt-2 rounded border p-2 bg-emerald-50">
    <div class="font-medium">{{ ref.title || '未命名资料' }}</div>
    <div class="text-gray-500">{{ ref.category }} · 相关度 {{ Number(ref.score).toFixed(2) }}</div>
    <div class="mt-1 text-gray-700 whitespace-pre-wrap">{{ ref.text }}</div>
  </div>
</details>
```

## 验收标准

1. RAG 有命中时，AI 回答下方显示“参考资料 N 条”。
2. 展开后能看到标题、分类、相关度、原文片段。
3. RAG 无命中时不显示参考资料区域。
4. AI 回答仍然保持流式输出。
5. 旧的 Markdown 渲染不受影响。

## 测试建议

问题示例：

- “云隐山有什么历史故事？”
- “云隐山春天有什么自然景观？”
- “有什么当地美食？”

检查参考资料分类是否与兴趣模式一致。

## 风险点

- SSE 格式容易与 Spring WebFlux 自动编码冲突，需要实际观察浏览器收到的流。
- 如果改动较大，建议先用 JSON 行协议，前端解析更简单。
