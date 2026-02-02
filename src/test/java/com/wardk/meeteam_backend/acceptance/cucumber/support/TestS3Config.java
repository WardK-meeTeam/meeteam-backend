package com.wardk.meeteam_backend.acceptance.cucumber.support;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트용 S3 설정
 * <p>
 * 기존 S3Config의 AmazonS3 빈을 WireMock(FakeS3Server) 엔드포인트로
 * 오버라이드합니다. path-style access를 활성화하여 WireMock이
 * {@code http://localhost:PORT/bucket/key} 형태로 요청을 받을 수 있도록 합니다.
 */
@TestConfiguration
public class TestS3Config {

    @Bean
    @Primary
    public AmazonS3 amazonS3(FakeS3Server fakeS3Server) {
        BasicAWSCredentials credentials = new BasicAWSCredentials("test-access-key", "test-secret-key");

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(fakeS3Server.getEndpoint(), "us-east-1"))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}