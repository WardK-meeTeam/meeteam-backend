package com.wardk.meeteam_backend.global.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class P6SpyFormatter implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, 
                               String prepared, String sql, String url) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        
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
