# 06 数字人配置前端页面

## 目标

为已有数字人配置后端 API 增加管理端前端页面，使管理员可以配置数字人形象、声音、语速和欢迎语等。

## 当前情况

后端已有数字人配置实体和 CRUD API，但 `admin.html` 只有知识库管理，没有数字人配置页面。

## 涉及文件

- `src/main/resources/static/admin.html`
- 可选：`src/main/resources/static/index.html`，用于读取启用配置并影响聊天页展示。

## 实施步骤

### 步骤 1：侧边栏增加入口

在 `admin.html` 侧边栏增加：

```html
<div class="text-xs text-gray-500 px-2 py-1 uppercase font-semibold">数字人管理</div>
<button @click="activeTab = 'digitalHuman'">🧍 数字人配置</button>
```

### 步骤 2：增加状态变量

在 Vue `setup()` 中增加：

```js
const digitalHumans = ref([]);
const digitalHumanForm = ref({
  name: '',
  appearanceStyle: '',
  clothing: '',
  voice: '',
  speechRate: '',
  background: '',
  welcomeMessage: '',
  active: false
});
const editingDigitalHumanId = ref(null);
```

字段名称需要以实际 `DigitalHumanConfig` 实体为准。

### 步骤 3：增加列表和表单 UI

页面包含：

- 配置列表
- 新增/编辑表单
- 启用按钮
- 删除按钮

字段建议：

- 名称
- 外观风格
- 服装
- 声音
- 语速
- 背景
- 欢迎语
- 是否启用

### 步骤 4：对接 API

需要按后端实际接口确认路径，预计为：

- `GET /api/admin/digital-human`
- `POST /api/admin/digital-human`
- `PUT /api/admin/digital-human/{id}`
- `DELETE /api/admin/digital-human/{id}`
- `POST /api/admin/digital-human/{id}/activate`

所有请求带：

```js
Authorization: `Bearer ${token.value}`
```

### 步骤 5：聊天页读取启用配置（可选但建议）

如果后端有公开只读接口，例如：

```text
GET /api/digital-human/active
```

则在 `index.html` 登录后加载配置，用于：

- 欢迎语
- 数字人名称
- TTS voice
- 语速
- 外观样式

## 验收标准

1. 管理后台能进入“数字人配置”页。
2. 能新增配置。
3. 能编辑配置。
4. 能删除配置。
5. 能启用某个配置。
6. 聊天页能使用启用配置中的欢迎语或声音。

## 风险点

- 前端字段名必须和后端实体/DTO 一致。
- 如果后端没有 DTO，直接暴露 Entity 时要注意字段大小写。
- 启用逻辑要保证同一时间只有一个 active 配置。
