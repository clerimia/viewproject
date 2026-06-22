package com.aicust.controller;

import com.aicust.dto.FeedbackRequest;
import com.aicust.repository.InteractionLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 游客侧交互反馈接口。 */
@RestController
@RequestMapping("/api/interactions")
public class InteractionFeedbackController {

    private final InteractionLogRepository logRepository;

    public InteractionFeedbackController(InteractionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @PostMapping("/{logId}/satisfaction")
    @Transactional
    public Map<String, Object> submitSatisfaction(@PathVariable Long logId,
                                                   @RequestBody FeedbackRequest request) {
        Integer score = request.getScore();
        if (score == null || score < 1 || score > 5) {
            return Map.of("success", false, "message", "评分必须在 1~5 之间");
        }

        Long userId = getCurrentUserId();
        int updated = logRepository.updateSatisfactionByIdAndUserId(logId, userId, score);
        if (updated == 0) {
            return Map.of("success", false, "message", "评价失败：记录不存在或不属于当前用户");
        }

        return Map.of("success", true, "message", "评价成功", "score", score);
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        return Long.valueOf(principal.toString());
    }
}
