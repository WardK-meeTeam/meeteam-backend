package com.wardk.meeteam_backend.acceptance.cucumber.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WireMock 기반 Fake S3 서버
 * <p>
 * 인수테스트에서 실제 AWS S3 대신 로컬 WireMock 서버를 사용하여
 * S3 PutObject / DeleteObject API를 스텁합니다.
 */
@Slf4j
@Component
public class FakeS3Server {

    private static final String BUCKET_PATH = "/test-bucket/.*";

    private WireMockServer wireMockServer;

    @PostConstruct
    public void start() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .dynamicPort());
        wireMockServer.start();
        stubS3Apis();
        log.info("FakeS3Server started on port {}", wireMockServer.port());
    }

    @PreDestroy
    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("FakeS3Server stopped");
        }
    }

    public int getPort() {
        return wireMockServer.port();
    }

    public String getEndpoint() {
        return "http://localhost:" + getPort();
    }

    /**
     * 시나리오 간 요청 기록 초기화
     * 스텁은 유지하고 기록된 요청만 리셋합니다.
     */
    public void reset() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetRequests();
        }
    }

    /**
     * 업로드(PutObject) 요청 횟수 조회
     */
    public int getUploadCount() {
        return wireMockServer.findAll(
                WireMock.putRequestedFor(WireMock.urlPathMatching(BUCKET_PATH))
        ).size();
    }

    private void stubS3Apis() {
        stubS3PutObject();
        stubS3DeleteObject();
    }

    /**
     * S3 PutObject 스텁 - 모든 PUT 요청에 200 응답
     */
    private void stubS3PutObject() {
        wireMockServer.stubFor(WireMock.put(WireMock.urlPathMatching(BUCKET_PATH))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("ETag", "\"fake-etag\"")));
    }

    /**
     * S3 DeleteObject 스텁 - 모든 DELETE 요청에 204 응답
     */
    private void stubS3DeleteObject() {
        wireMockServer.stubFor(WireMock.delete(WireMock.urlPathMatching(BUCKET_PATH))
                .willReturn(WireMock.aResponse()
                        .withStatus(204)));
    }
}