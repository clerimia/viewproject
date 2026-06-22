# 01 前端兴趣模式选择与 `mode` 参数传递

## 目标

让游客在聊天前选择兴趣偏好，并将选择结果通过 `mode` 字段传给后端，使后端 RAG 分类过滤真正生效。

## 当前情况

后端已经具备完整能力：

- `ChatRequest` 已有 `mode` 字段。
- `ChatController` 已将 `req.getMode()` 传给 `AiChatService.streamChat()`。
- `AiChatService` 已将 `mode` 转换为分类。
- `RagSearchService` 已根据分类拼接 RAG filter。

但前端 `src/main/resources/static/index.html` 发送聊天请求时只传：

```js
body: JSON.stringify({ prompt: userText })
```

因此后端永远拿不到用户选择的兴趣模式。

## 涉及文件

- `src/main/resources/static/index.html`

## 实施步骤

### 步骤 1：增加兴趣模式状态

在 Vue `setup()` 中增加：

```js
const interestMode = ref('all');
```

定义模式列表：

```js
const interestModes = [
  { key: 'all', label: '综合', icon: '✨' },
  { key: 'history', label: '历史文化', icon: '🏯' },
  { key: 'nature', label: '自然风光', icon: '🌲' },
  { key: 'food', label: '美食特产', icon: '🍜' }
];
```

### 步骤 2：增加按钮 UI

在聊天输入区上方或顶部栏下方增加一排按钮。

建议位置：`chatContainer` 和输入框之间，或输入框上方。

按钮示例：

```html
<div class="max-w-4xl mx-auto mb-3 flex flex-wrap gap-2">
  <button v-for="mode in interestModes"
          :key="mode.key"
          @click="interestMode = mode.key"
          :class="interestMode === mode.key ? '...' : '...'">
    {{ mode.icon }} {{ mode.label }}
  </button>
</div>
```

### 步骤 3：发送聊天请求时传入 `mode`

将请求体改为：

```js
body: JSON.stringify({
  prompt: userText,
  mode: interestMode.value
})
```

### 步骤 4：返回模板变量

确保 `setup()` return 中包含：

```js
interestMode,
interestModes
```

## 验收标准

1. 页面能看到四个兴趣按钮：综合、历史文化、自然风光、美食特产。
2. 点击按钮后有明显选中态。
3. 发送聊天请求时：
   - 综合：`mode = all`
   - 历史文化：`mode = history`
   - 自然风光：`mode = nature`
   - 美食特产：`mode = food`
4. 后端日志中 RAG category 能对应变成：
   - `历史文化`
   - `自然风光`
   - `美食特产`
   - 或 `null`

## 测试建议

打开浏览器开发者工具 Network，检查 `/api/chat` 请求体是否包含 `mode`。

## 风险点

- 当前页面还有 `currentMode` 表示 `chat/agent`，不要和兴趣 `mode` 混淆。
- 建议变量命名为 `interestMode`，避免覆盖现有 `currentMode`。
