package com.wardk.meeteam_backend.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 관련 외부 설정을 관리하는 Properties 클래스
 * application.yml의 app.oauth2 설정을 바인딩
 */
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    //리다이렉트 관련 설정
    private final Redirect redirect;

    //프로바이더별 설정
    private final Providers providers;

    @Getter
    @RequiredArgsConstructor
    public static class Redirect {
        private final String loginSuccessUrl;       // 기존 회원 로그인 성공 시 이동할 URL
        private final String oauth2SignupUrl;       // 신규 회원 추가 정보 입력 페이지 URL
        private final String failureUrl;            // 로그인 실패 시 이동할 URL
    }

    @Getter
    @RequiredArgsConstructor
    public static class Providers {
        private final Google google;
        private final Github github;

        @Getter
        @RequiredArgsConstructor
        public static class Google {
            private final String name;
            private final String userNameAttribute;
            private final String emailAttribute;
            private final String idAttribute;
        }

        @Getter
        @RequiredArgsConstructor
        public static class Github {
            private final String name;
            private final String userNameAttribute;
            private final String emailAttribute;
            private final String idAttribute;
        }
    }
}