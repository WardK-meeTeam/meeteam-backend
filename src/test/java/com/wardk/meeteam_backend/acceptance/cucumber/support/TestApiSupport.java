package com.wardk.meeteam_backend.acceptance.cucumber.support;

import com.wardk.meeteam_backend.acceptance.cucumber.api.AuthApi;
import com.wardk.meeteam_backend.acceptance.cucumber.api.ProjectApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * API 호출 클래스 통합 접근
 * 각 도메인별 API 클래스에 쉽게 접근할 수 있도록 합니다.
 */
@Getter
@Component
@RequiredArgsConstructor
public class TestApiSupport {

    private final AuthApi auth;
    private final ProjectApi project;


    // 필요한 도메인 API가 추가되면 여기에 추가
    // private final ApplicationApi application;
    // private final FileApi file;
    // private final NotificationApi notification;
}
