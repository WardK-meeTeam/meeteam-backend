package com.wardk.meeteam_backend.global.config;

import com.p6spy.engine.spy.P6SpyOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("dev") // 개발 환경에서만 활성화
public class P6SpyConfig {

    @PostConstruct
    public void setP6SpyOptions() {
        // 로그 포맷 설정
        P6SpyOptions.getActiveInstance().setLogMessageFormat(P6SpyFormatter.class.getName());

        // 실행 시간 임계값 설정 (0으로 설정하여 모든 쿼리 로깅)
        System.setProperty("p6spy.config.executionThreshold", "0");

        // 카테고리 설정
        System.setProperty("p6spy.config.excludecategories", "info,debug,batch");
        System.setProperty("p6spy.config.includecategories", "error,warn,statement,commit,rollback,result");

        // 파라미터 값 포함
        System.setProperty("p6spy.config.includeParameterValues", "true");
    }
}