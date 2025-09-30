package com.wardk.meeteam_backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityUrls {
    // 인증이 필요 없는 화이트리스트 경로들
    public List<String> whitelist;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 주어진 URI 가 화이트리스트에 포함되는지 확인
    public boolean isWhitelisted(String uri) {
        return whitelist.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}