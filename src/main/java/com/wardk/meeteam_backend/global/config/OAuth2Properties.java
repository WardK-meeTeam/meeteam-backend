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

        //OAuth2 성공 시 프론트엔드 베이스 URL
        private final String successBaseUrl;

        //OAuth2 성공 시 리다이렉트 경로
        private final String successPath;

        //OAuth2 실패 시 프론트엔드 베이스 URL
        private final String failureBaseUrl;

        //OAuth2 실패 시 리다이렉트 경로
        private final String failurePath;

        //OAuth2 실패 처리 엔드포인트
        private final String failureEndpoint;

        //성공 시 완전한 리다이렉트 URL 생성
        public String getSuccessUrl() {
            return successBaseUrl + successPath;
        }

        //실패 시 완전한 리다이렉트 URL 생성
        public String getFailureUrl() {
            return failureBaseUrl + failurePath;
        }

        //토큰과 함께 성공 리다이렉트 URL 생성
        public String getSuccessUrlWithToken(String token) {
            return getSuccessUrl() + "?token=" + token;
        }

        //에러와 함께 실패 리다이렉트 URL 생성
        public String getFailureUrlWithError(String error) {
            return getFailureUrl() + "?error=" + error;
        }
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
            private final String loginAttribute;
            private final String emailAttribute;
            private final String idAttribute;
        }
    }
}