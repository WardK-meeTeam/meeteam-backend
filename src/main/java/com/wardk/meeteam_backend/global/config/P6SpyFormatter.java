package com.wardk.meeteam_backend.global.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class P6SpyFormatter implements MessageFormattingStrategy {

    // 로깅 중복 방지를 위한 최근 쿼리 캐시
    private static final Set<String> recentQueryCache = Collections.synchronizedSet(new HashSet<>());
    private static final int CACHE_SIZE_LIMIT = 100;

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, 
                               String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        
        // 중복 방지를 위한 키 생성
        String cacheKey = sql.trim() + connectionId;
        
        // 캐시 크기 제한
        if (recentQueryCache.size() > CACHE_SIZE_LIMIT) {
            recentQueryCache.clear();
        }
        
        // 중복 검사
        if (!recentQueryCache.add(cacheKey)) {
            // 이미 최근에 로깅된 쿼리는 무시
            return "";
        }
        
        // 기존 로직 유지
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // SQL 포맷팅
        String formattedSql = formatSql(category, sql);
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n------------------------------------------------------------------------------------------");
        sb.append("\n[SQL] ").append(format.format(new Date()));
        sb.append(" | ").append(elapsed).append("ms");
        sb.append(" | ").append(category);
        sb.append(" | connection ").append(connectionId);
        if (formattedSql.trim().toLowerCase(Locale.ROOT).startsWith("select")) {
            sb.append("\n[SELECT 쿼리]\n");
        } else if (formattedSql.trim().toLowerCase(Locale.ROOT).startsWith("insert")) {
            sb.append("\n[INSERT 쿼리]\n");
        } else if (formattedSql.trim().toLowerCase(Locale.ROOT).startsWith("update")) {
            sb.append("\n[UPDATE 쿼리]\n");
        } else if (formattedSql.trim().toLowerCase(Locale.ROOT).startsWith("delete")) {
            sb.append("\n[DELETE 쿼리]\n");
        } else {
            sb.append("\n[기타 쿼리]\n");
        }
        sb.append(formattedSql);
        sb.append("\n------------------------------------------------------------------------------------------");
        
        return sb.toString();
    }
    
    private String formatSql(String category, String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        // DDL은 HibernateSQL 포맷으로 출력
        if (category.contains("statement") && sql.trim().toLowerCase(Locale.ROOT).startsWith("create")) {
            return FormatStyle.DDL.getFormatter().format(sql);
        }
        
        // DML은 HibernateSQL 포맷으로 출력
        if (category.contains("statement") && (
            sql.trim().toLowerCase(Locale.ROOT).startsWith("select") ||
            sql.trim().toLowerCase(Locale.ROOT).startsWith("insert") ||
            sql.trim().toLowerCase(Locale.ROOT).startsWith("update") ||
            sql.trim().toLowerCase(Locale.ROOT).startsWith("delete"))) {
            return FormatStyle.BASIC.getFormatter().format(sql);
        }
        
        return sql;
    }
}
