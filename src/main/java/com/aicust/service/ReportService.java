package com.aicust.service;

import com.aicust.dto.SatisfactionTrendPoint;
import com.aicust.dto.SentimentDistribution;
import com.aicust.dto.SentimentReport;
import com.aicust.dto.ServiceDashboard;
import com.aicust.repository.InteractionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 聚合报表服务 — 提供日报/周报所需的各类统计查询。
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InteractionLogRepository logRepository;
    private final SentimentAnalysisService sentimentService;

    public ReportService(InteractionLogRepository logRepository,
                         SentimentAnalysisService sentimentService) {
        this.logRepository = logRepository;
        this.sentimentService = sentimentService;
    }

    // ======================== 服务看板 ========================

    /**
     * 获取当日/本周服务概览。
     */
    @Transactional(readOnly = true)
    public ServiceDashboard getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();

        long todayVisitors = logRepository.countByCreatedAtBetween(todayStart, now);
        long weekVisitors = logRepository.countByCreatedAtBetween(weekStart, now);

        // 热门问答 Top10
        List<Map<String, Object>> hotQa = logRepository.topQuestionsInRange(
                        weekStart, now, PageRequest.of(0, 10))
                .stream()
                .map(row -> Map.<String, Object>of(
                        "question", row[0],
                        "count", row[1]
                ))
                .collect(Collectors.toList());

        // 满意度趋势（最近7天）
        List<SatisfactionTrendPoint> trend = buildSatisfactionTrendFast(now);

        // 各景点关注度分布
        List<Map<String, Object>> attractionDist = buildAttractionDistribution(todayStart, now);

        return ServiceDashboard.builder()
                .todayVisitors(todayVisitors)
                .weekVisitors(weekVisitors)
                .hotQaTop10(hotQa)
                .satisfactionTrend(trend)
                .attractionDistribution(attractionDist)
                .build();
    }

    // ======================== 情感分析报告 ========================

    /**
     * 生成情感分析报告。
     *
     * @param days 回溯天数，默认7
     */
    @Transactional(readOnly = true)
    public SentimentReport getSentimentReport(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);

        long total = logRepository.countByCreatedAtBetween(start, end);
        if (total == 0) {
            return SentimentReport.builder()
                    .sentiment(SentimentDistribution.builder().build())
                    .focusClusters(Collections.emptyList())
                    .satisfactionTrend(Collections.emptyList())
                    .attractionScores(Collections.emptyList())
                    .build();
        }

        // 1. 情感分布
        SentimentDistribution dist = analyzeSentimentDistribution(start, end);

        // 2. 关注点聚类
        Map<String, Long> focusPoints = aggregateFocusPoints(start, end);
        List<Map<String, Object>> clusters = buildFocusClusters(focusPoints);

        // 3. 满意度趋势（近7天）
        List<Map<String, Object>> trend = buildSatisfactionTrendMapFast(end);

        // 4. 各景点关注度
        List<Map<String, Object>> attractions = buildAttractionScores(focusPoints);

        return SentimentReport.builder()
                .sentiment(dist)
                .focusClusters(clusters)
                .satisfactionTrend(trend)
                .attractionScores(attractions)
                .build();
    }

    // ======================== 私有辅助方法 ========================

    private SentimentDistribution analyzeSentimentDistribution(LocalDateTime start, LocalDateTime end) {
        long positive = 0, neutral = 0, negative = 0;
        double totalScore = 0.0;
        long scored = 0;

        for (Object[] row : logRepository.sentimentStatsInRange(start, end)) {
            String label = (String) row[0];
            long count = ((Number) row[1]).longValue();
            Number avgScore = (Number) row[2];
            long scoredCount = row[3] == null ? 0 : ((Number) row[3]).longValue();

            if (label == null) {
                continue;
            }
            switch (label) {
                case "POSITIVE" -> positive += count;
                case "NEGATIVE" -> negative += count;
                default -> neutral += count;
            }
            if (avgScore != null && scoredCount > 0) {
                totalScore += avgScore.doubleValue() * scoredCount;
                scored += scoredCount;
            }
        }

        for (String answer : logRepository.answersWithoutSentimentInRange(start, end)) {
            SentimentAnalysisService.AnalysisResult result = sentimentService.analyze(answer);
            if (result.score() > 0.05) positive++;
            else if (result.score() < -0.05) negative++;
            else neutral++;
            totalScore += result.score();
            scored++;
        }

        double avgScore = scored > 0 ? totalScore / scored : 0.0;
        long total = positive + neutral + negative;

        return SentimentDistribution.builder()
                .positive(positive)
                .neutral(neutral)
                .negative(negative)
                .positiveRatio(total > 0 ? (double) positive / total : 0.0)
                .avgSentimentScore(Math.round(avgScore * 1000.0) / 1000.0)
                .build();
    }

    private List<Map<String, Object>> buildFocusClusters(Map<String, Long> allFocus) {
        return allFocus.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(e -> Map.<String, Object>of(
                        "keyword", e.getKey(),
                        "count", e.getValue()
                ))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildAttractionScores(Map<String, Long> scoreMap) {
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .map(e -> Map.<String, Object>of(
                        "attraction", e.getKey(),
                        "mentions", e.getValue()
                ))
                .collect(Collectors.toList());
    }

    private List<SatisfactionTrendPoint> buildSatisfactionTrendFast(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<SatisfactionTrendPoint> result = new ArrayList<>();

        for (Object[] row : logRepository.satisfactionTrendStatsInRange(start, end)) {
            String date = formatDateKey(row[0]);
            long totalCount = ((Number) row[1]).longValue();
            Number avgScore = (Number) row[2];
            Number positiveCount = (Number) row[4];

            result.add(SatisfactionTrendPoint.builder()
                    .date(date)
                    .avgScore(avgScore == null ? 0.0 : Math.round(avgScore.doubleValue() * 100.0) / 100.0)
                    .totalCount(totalCount)
                    .positiveRatio(totalCount > 0 && positiveCount != null
                            ? Math.round(positiveCount.doubleValue() / totalCount * 1000.0) / 1000.0
                            : 0.0)
                    .build());
        }

        return result;
    }

    private List<Map<String, Object>> buildSatisfactionTrendMapFast(LocalDateTime now) {
        return buildSatisfactionTrendFast(now).stream()
                .map(point -> Map.<String, Object>of(
                        "date", point.getDate(),
                        "avgScore", point.getAvgScore(),
                        "totalCount", point.getTotalCount(),
                        "positiveRatio", point.getPositiveRatio()
                ))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildAttractionDistribution(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> scoreMap = aggregateFocusPoints(start, end);
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of(
                        "name", e.getKey(),
                        "count", e.getValue(),
                        "score", e.getValue()
                ))
                .collect(Collectors.toList());
    }

    private Map<String, Long> aggregateFocusPoints(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> allFocus = new HashMap<>();

        for (String focusPoints : logRepository.focusPointsInRange(start, end)) {
            mergeFocusPointString(allFocus, focusPoints);
        }

        for (Object[] row : logRepository.questionAnswersWithoutFocusPointsInRange(start, end)) {
            String question = row[0] == null ? "" : row[0].toString();
            String answer = row[1] == null ? "" : row[1].toString();
            sentimentService.extractFocusPoints(question, answer)
                    .forEach((key, count) -> allFocus.merge(key, count, Long::sum));
        }

        return allFocus;
    }

    private void mergeFocusPointString(Map<String, Long> target, String focusPoints) {
        if (focusPoints == null || focusPoints.isBlank()) {
            return;
        }
        Arrays.stream(focusPoints.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(word -> target.merge(word, 1L, Long::sum));
    }

    private String formatDateKey(Object dateValue) {
        if (dateValue == null) {
            return "";
        }
        if (dateValue instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().format(DATE_FMT);
        }
        if (dateValue instanceof LocalDate localDate) {
            return localDate.format(DATE_FMT);
        }
        if (dateValue instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate().format(DATE_FMT);
        }
        return dateValue.toString();
    }
}
