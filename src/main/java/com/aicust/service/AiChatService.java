package com.aicust.service;

import com.aicust.model.AiPlan;
import com.aicust.model.InteractionLog;
import com.aicust.service.SentimentAnalysisService.AnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final ChatClient chatClient;
    private final TokenEstimator estimator;
    private final TokenQuotaService quotaService;
    private final PlanService planService;
    private final SensitiveWordService sensitiveService;
    private final ChatMemoryService memoryService;
    private final RagSearchService ragSearchService;
    private final InteractionLogWriter logWriter;
    private final SentimentAnalysisService sentimentService;
    private final ObjectMapper objectMapper;

    /** RAG 检索默认 topK */
    private static final int RAG_TOPK = 5;

    public AiChatService(ChatClient chatClient, TokenEstimator estimator, TokenQuotaService quotaService,
                         PlanService planService, SensitiveWordService sensitiveService,
                         ChatMemoryService memoryService, RagSearchService ragSearchService,
                         InteractionLogWriter logWriter, SentimentAnalysisService sentimentService,
                         ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.estimator = estimator;
        this.quotaService = quotaService;
        this.planService = planService;
        this.sensitiveService = sensitiveService;
        this.memoryService = memoryService;
        this.ragSearchService = ragSearchService;
        this.logWriter = logWriter;
        this.sentimentService = sentimentService;
        this.objectMapper = objectMapper;
    }

    /**
     * 流式对话（带 RAG 检索增强）。
     *
     * @param userId 用户ID
     * @param prompt 用户问题
     * @param mode   兴趣模式（"history"/"nature"/"food"/null），用于 RAG 分类过滤
     */
    public Flux<ServerSentEvent<String>> streamChat(Long userId, String prompt, String mode) {

        if (sensitiveService.hasSensitiveWord(prompt)) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("message")
                    .data("Your prompt contains sensitive content. Request denied.")
                    .build());
        }

        long startTime = System.currentTimeMillis();
        AtomicReference<String> usernameRef = new AtomicReference<>("");
        AtomicReference<String> fullAnswerRef = new AtomicReference<>("");

        // 尝试获取用户名
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof com.aicust.model.User user) {
                usernameRef.set(user.getUsername());
            }
        } catch (Exception ignored) {
        }

        // ===== RAG 检索 =====
        String category = modeToCategory(mode);
        List<RagSearchService.SearchHit> ragHits = ragSearchService.search(prompt, RAG_TOPK, category);
        String systemPrompt = buildSystemPrompt(ragHits, category);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(memoryService.getRelatedHistory(userId, prompt));
        messages.add(new UserMessage(prompt));

        String model = "qwen";
        int estimated = estimator.estimate(model, prompt);
        AiPlan plan = planService.getPlan(userId);
        quotaService.check(userId, plan.getDailyTokenLimit(), estimated);

        InteractionLog pendingLog = logWriter.createPending(userId, usernameRef.get(), prompt, mode);
        Long interactionLogId = pendingLog.getId();

        memoryService.addMessage(userId, new UserMessage(prompt));

        AtomicInteger actualLength = new AtomicInteger(0);

        Flux<ServerSentEvent<String>> metaEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("meta")
                        .data(toJson(Map.of("interactionLogId", interactionLogId)))
                        .build()
        );

        Flux<ServerSentEvent<String>> referencesEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("references")
                        .data(toReferencesJson(ragHits))
                        .build()
        );

        Flux<ServerSentEvent<String>> answerEvents = chatClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        actualLength.addAndGet(chunk.length());
                        fullAnswerRef.set(fullAnswerRef.get() + chunk);
                    }
                })
                .doOnComplete(() -> {
                    int actual = actualLength.get();
                    long duration = System.currentTimeMillis() - startTime;
                    String answer = normalizeFinalAnswer(fullAnswerRef.get());

                    quotaService.settle(userId, estimated, actual);
                    memoryService.addMessage(userId, new AssistantMessage(answer));

                    // 异步补全交互日志 + 情感分析
                    saveInteractionLog(interactionLogId, userId, usernameRef.get(), prompt, answer, mode,
                            estimated, actual, duration);
                })
                .doOnError(e -> {
                    quotaService.rollback(userId, estimated);
                    log.error("Chat error for userId={}: {}", userId, e.getMessage());
                })
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(chunk == null ? "" : chunk)
                        .build());

        return Flux.concat(metaEvent, referencesEvent, answerEvents);
    }

    /**
     * 异步保存交互日志并进行情感分析。
     */
    private void saveInteractionLog(Long logId, Long userId, String username, String question,
                                    String answer, String mode,
                                    int estimatedTokens, int actualTokens,
                                    long durationMs) {
        try {
            // 情感分析
            AnalysisResult sentiment = sentimentService.analyze(answer);

            // 关注点聚类
            Map<String, Long> focusPoints = sentimentService.extractFocusPoints(question, answer);
            String focusStr = focusPoints.keySet().stream()
                    .limit(10)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            logWriter.completeInteraction(logId, answer,
                    estimatedTokens, actualTokens, durationMs,
                    sentiment.score(), sentiment.label(), focusStr);

        } catch (Exception e) {
            log.error("Failed to save interaction log for userId={}: {}", userId, e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize SSE payload: {}", e.getMessage());
            return "{}";
        }
    }

    private String toReferencesJson(List<RagSearchService.SearchHit> hits) {
        try {
            List<Map<String, Object>> refs = new ArrayList<>();
            for (int i = 0; i < hits.size(); i++) {
                RagSearchService.SearchHit hit = hits.get(i);
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("id", i + 1);
                ref.put("chunkId", safeText(hit.chunkId(), 80));
                ref.put("title", safeText(hit.title(), 120));
                ref.put("category", safeText(hit.category(), 40));
                ref.put("score", Math.round(hit.fusedScore() * 100.0) / 100.0);
                ref.put("snippet", safeText(hit.text(), 260));
                refs.add(ref);
            }
            return objectMapper.writeValueAsString(refs);
        } catch (Exception e) {
            log.warn("Failed to serialize RAG references: {}", e.getMessage());
            return "[]";
        }
    }

    private String safeText(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() <= maxLen ? normalized : normalized.substring(0, maxLen) + "...";
    }

    /**
     * 构建带 RAG 检索结果的 system prompt。
     * <p>有检索结果时要求 LLM 严格基于参考资料回答；无结果时降级为通用回答。
     */
    private String buildSystemPrompt(List<RagSearchService.SearchHit> hits, String category) {
        // 关键要求：禁止输出任何 Markdown 符号，否则 TTS 会读出来
        String noFormat = "\n重要：回答时不要使用任何格式符号，如 # * - > [ ] { } | 等，也不要使用 Markdown 语法。正式回答请输出纯文本的自然语言，方便页面展示和语音朗读。";

        if (hits.isEmpty()) {
            String catHint = category != null ? "关于" + category + "方面的" : "";
            return "你是一个景区智能助手。"
                    + "请尽力回答游客的" + catHint + "问题。"
                    + "如果遇到不确定的信息，请如实告知游客并建议其咨询景���工作人员。"
                    + noFormat;
        }

        String catHint = category != null ? "（" + category + "）" : "";
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个景区智能助手，正在为该景区" + catHint + "的游客提供服务。\n\n");
        sb.append("请严格基于以下参考资料回答游客问题，不要编造参考资料中没有的信息。\n");
        sb.append("如果参考资料不足以回答问题，请如实告知游客。\n\n");
        sb.append("=== 参考资料（共").append(hits.size()).append("条） ===\n");

        for (int i = 0; i < hits.size(); i++) {
            RagSearchService.SearchHit hit = hits.get(i);
            sb.append("参考").append(i + 1).append("：");
            if (!hit.title().isBlank()) {
                sb.append(hit.title()).append(" - ");
            }
            sb.append(hit.text()).append("\n\n");
        }
        sb.append("=== 参考资料结束 ===\n\n");
        sb.append("请用自然、亲切的语气回答游客。");
        sb.append(noFormat);

        return sb.toString();
    }

    private String normalizeFinalAnswer(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?is)</?think>", "")
                .trim();
    }

    /** 将前端 mode 转为 RAG category */
    private String modeToCategory(String mode) {
        if (mode == null || mode.isBlank() || "all".equalsIgnoreCase(mode)) {
            return null;
        }
        return switch (mode.toLowerCase()) {
            case "history" -> "历史文化";
            case "nature" -> "自然风光";
            case "food" -> "美食特产";
            default -> null;
        };
    }
}