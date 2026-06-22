# 执行方案索引

本目录用于拆分记录景区导览 AI 数字人项目后续代码实现计划。每个 Markdown 文件对应一个待完成点，包含目标、涉及文件、实施步骤、验收标准和风险点。

## P0：核心演示必须完成

1. [前端兴趣模式选择与 mode 参数传递](./01-interest-mode.md)
2. [前端景区品牌化改造](./02-scenic-branding.md)
3. [RAG 参考资料前端展示闭环](./03-rag-references.md)
4. [知识库真实导入与 RAG 联调验证](./04-rag-import-verification.md)

## P1：管理后台与数据展示

5. [知识库管理列表/搜索/删除真实功能](./05-knowledge-management-crud.md)
6. [数字人配置前端页面](./06-digital-human-config-frontend.md)
7. [数据大屏前端页面](./07-dashboard-frontend.md)

## P2：体验增强与演示优化

8. [数字人说话/聆听/思考状态动画](./08-digital-human-animation.md)
9. [前端去通用客服化与导览模式改造](./09-scenic-mode-refactor.md)
10. [满意度评价入口](./10-satisfaction-feedback.md)

## 建议实施顺序

1. 先完成 `01-interest-mode.md`，确保个性化 RAG 过滤真正从前端生效。
2. 再完成 `02-scenic-branding.md`，让产品外观符合景区数字人主题。
3. 再完成 `03-rag-references.md`，形成“检索 → 生成 → 可追溯展示”的核心闭环。
4. 再执行 `04-rag-import-verification.md`，确认知识库导入与检索可用。
5. 然后补齐 `07-dashboard-frontend.md` 和 `06-digital-human-config-frontend.md`。
6. 最后完善 `05-knowledge-management-crud.md`、`08`、`09`、`10` 等体验和管理能力。
