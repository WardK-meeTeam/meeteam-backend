package com.wardk.meeteam_backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityUrls {
    // 인증이 필요 없는 화이트리스트 경로들 (새로운 형식)
    private List<WhitelistEntry> whitelist;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 주어진 URI와 HTTP Method가 화이트리스트에 포함되는지 확인
    public boolean isWhitelisted(String uri, String method) {
        return whitelist.stream()
                .anyMatch(entry -> {
                    boolean uriMatches = pathMatcher.match(entry.getUri(), uri);
                    boolean methodMatches = "*".equals(entry.getMethod()) ||
                                          entry.getMethod().equalsIgnoreCase(method);
                    return uriMatches && methodMatches;
                });
    }

    // 기존 호환성을 위한 메서드 (URI만 체크, 모든 HTTP Method 허용)
    public boolean isWhitelisted(String uri) {
        return isWhitelisted(uri, "*");
    }

    // Spring Security 설정에서 사용할 URI 패턴 목록 반환 (기존 호환성용)
    public List<String> getUriPatterns() {
        return whitelist.stream()
                .map(WhitelistEntry::getUri)
                .distinct()
                .collect(Collectors.toList());
    }

    // Spring Security용 RequestMatcher 배열 반환 (HTTP Method 고려)
    public RequestMatcher[] getRequestMatchers() {
        return whitelist.stream()
                .map(entry -> {
                    if ("*".equals(entry.getMethod())) {
                        // 모든 HTTP Method 허용
                        return new AntPathRequestMatcher(entry.getUri());
                    } else {
                        // 특정 HTTP Method만 허용
                        HttpMethod httpMethod = HttpMethod.valueOf(entry.getMethod().toUpperCase());
                        return new AntPathRequestMatcher(entry.getUri(), httpMethod.name());
                    }
                })
                .toArray(RequestMatcher[]::new);
    }
}