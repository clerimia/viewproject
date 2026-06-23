package com.aicust.repository;

import com.aicust.model.InteractionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {

    /** 查询指定时间段内的所有交互日志 */
    List<InteractionLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** 按满意度分组统计 */
    @Query("SELECT i.satisfaction, COUNT(i) FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end GROUP BY i.satisfaction ORDER BY i.satisfaction")
    List<Object[]> countBySatisfactionInRange(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    /** 热门问题 TopN（按被评价次数降序） */
    @Query("SELECT i.question, COUNT(i) FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.question ORDER BY COUNT(i) DESC")
    List<Object[]> topQuestionsInRange(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       Pageable pageable);

    /** 用户最近交互日志 */
    List<InteractionLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 更新满意度评分 */
    @Modifying
    @Query("UPDATE InteractionLog i SET i.satisfaction = :score WHERE i.id = :id")
    int updateSatisfaction(@Param("id") Long id, @Param("score") Integer score);

    /** 游客只能更新自己的交互日志满意度 */
    @Modifying
    @Query("UPDATE InteractionLog i SET i.satisfaction = :score WHERE i.id = :id AND i.userId = :userId")
    int updateSatisfactionByIdAndUserId(@Param("id") Long id,
                                        @Param("userId") Long userId,
                                        @Param("score") Integer score);

    /** 用户当日对话次数 */
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    /** 当日总对话数 */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** 按情感标签聚合统计 */
    @Query("SELECT i.sentimentLabel, COUNT(i), AVG(i.sentimentScore), COUNT(i.sentimentScore) FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end GROUP BY i.sentimentLabel")
    List<Object[]> sentimentStatsInRange(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /** 查询缺少情感标签的历史答案，用于兼容旧数据 */
    @Query("SELECT i.answer FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end AND i.sentimentLabel IS NULL")
    List<String> answersWithoutSentimentInRange(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /** 按日期聚合满意度趋势 */
    @Query("SELECT FUNCTION('DATE', i.createdAt), COUNT(i), AVG(i.satisfaction), COUNT(i.satisfaction), " +
           "SUM(CASE WHEN i.sentimentLabel = 'POSITIVE' THEN 1 ELSE 0 END) FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end GROUP BY FUNCTION('DATE', i.createdAt) " +
           "ORDER BY FUNCTION('DATE', i.createdAt)")
    List<Object[]> satisfactionTrendStatsInRange(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    /** 查询已持久化的关注点 */
    @Query("SELECT i.focusPoints FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end " +
           "AND i.focusPoints IS NOT NULL AND i.focusPoints <> ''")
    List<String> focusPointsInRange(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /** 查询缺少关注点的历史问答，用于兼容旧数据 */
    @Query("SELECT i.question, i.answer FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end " +
           "AND (i.focusPoints IS NULL OR i.focusPoints = '')")
    List<Object[]> questionAnswersWithoutFocusPointsInRange(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    /** 本周（周一到周日）每天的对话数 */
    @Query("SELECT FUNCTION('DATE', i.createdAt), COUNT(i) FROM InteractionLog i " +
           "WHERE i.createdAt BETWEEN :start AND :end GROUP BY FUNCTION('DATE', i.createdAt) " +
           "ORDER BY FUNCTION('DATE', i.createdAt)")
    List<Object[]> dailyCountInRange(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}
