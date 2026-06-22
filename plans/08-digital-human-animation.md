# 08 数字人说话/聆听/思考状态动画

## 目标

在不接入真实 2D 数字人 SDK 的情况下，先用前端状态和 CSS 动画实现数字人的演示效果：待机、聆听、思考、讲解。

## 当前情况

前端已有：

- 录音按钮
- ASR 上传识别
- TTS 播放按钮

但没有数字人形象区域和状态动画。

## 涉及文件

- `src/main/resources/static/index.html`

## 实施步骤

### 步骤 1：增加数字人状态变量

```js
const avatarState = ref('idle');
```

状态定义：

- `idle`：待机
- `listening`：聆听中
- `thinking`：思考中
- `speaking`：讲解中

### 步骤 2：在页面增加数字人卡片

建议放在聊天区域顶部或侧边栏：

```html
<div class="digital-human-card">
  <div :class="['avatar', avatarState]">
    🧑‍💼
  </div>
  <div>{{ avatarStatusText }}</div>
</div>
```

### 步骤 3：录音状态联动

- 开始录音时：`avatarState = 'listening'`
- 停止录音并识别时：`avatarState = 'thinking'`
- 识别完成后：根据是否自动发送决定状态

### 步骤 4：生成状态联动

- 发送消息后：`avatarState = 'thinking'`
- 收到第一个 token 后：可以保持 thinking 或切 speaking
- 生成结束后：`avatarState = 'idle'`

### 步骤 5：TTS 播放状态联动

- 播放开始：`avatarState = 'speaking'`
- 播放结束：`avatarState = 'idle'`

### 步骤 6：CSS 动画

建议动画：

- `idle`：轻微浮动
- `listening`：外圈脉冲
- `thinking`：小点跳动
- `speaking`：嘴部/头像缩放动画

## 验收标准

1. 页面有数字人形象卡片。
2. 点击录音时显示“聆听中”。
3. AI 回复生成时显示“思考中”。
4. 播放语音时显示“讲解中”，且有明显动画。
5. 操作结束后回到“待机”。

## 风险点

- 状态切换要放在 try/finally，避免异常后一直停留在某个状态。
- 多条 TTS 同时播放时要先停止旧音频。
