# 07 数据大屏前端页面

## 目标

新增数据大屏页面，展示景区 AI 数字人服务运营数据，包括服务人次、热门问答、满意度趋势和景点关注度分布。

## 当前情况

后端已有 `/api/admin/dashboard` 聚合接口，但静态资源目录中没有数据大屏页面。

## 涉及文件

- 新增：`src/main/resources/static/dashboard.html`
- 可选修改：`src/main/resources/static/admin.html` 增加跳转入口

## 页面方案

建议使用独立页面：

```text
/dashboard.html
```

使用 ECharts CDN：

```html
<script src="https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"></script>
```

## 实施步骤

### 步骤 1：新增页面结构

页面布局建议：

1. 顶部标题：云隐山 AI 数字人运营大屏
2. 指标卡片：
   - 今日服务人次
   - 本周服务人次
   - 平均满意度
   - 热门问题数量
3. 图表区：
   - 热门问答 Top10 柱状图
   - 满意度趋势折线图
   - 景点关注度分布饼图
4. 底部：最近更新时间和自动刷新提示

### 步骤 2：登录态处理

从 `localStorage` 读取 token：

```js
const token = localStorage.getItem('token');
```

请求时带：

```js
Authorization: `Bearer ${token}`
```

如果 401/403，则跳转首页。

### 步骤 3：请求后端数据

```js
const res = await fetch('/api/admin/dashboard', {
  headers: { Authorization: `Bearer ${token}` }
});
const data = await res.json();
```

### 步骤 4：渲染图表

根据 `ServiceDashboard` 实际返回字段适配：

- `todayVisitors`
- `weekVisitors`
- `hotQaTop10`
- `satisfactionTrend`
- `attractionDistribution`

如果字段为空，显示空状态。

### 步骤 5：自动刷新

```js
setInterval(loadDashboard, 30000);
```

每 30 秒刷新一次。

### 步骤 6：后台入口

在 `admin.html` 侧边栏或底部增加：

```html
<a href="/dashboard.html">📊 数据大屏</a>
```

## 验收标准

1. 登录后能访问 `/dashboard.html`。
2. 页面能成功调用 `/api/admin/dashboard`。
3. 指标卡片显示当日/本周服务人次。
4. 图表能展示热门问答、满意度趋势、景点关注度。
5. 页面能自动刷新。
6. 数据为空时不报错，有空状态提示。

## 风险点

- 后端字段结构需要实际确认后适配。
- ECharts CDN 需要外网访问；如比赛环境无外网，需要改为本地静态资源。
- 大屏页面要注意移动端和大屏分辨率兼容。
