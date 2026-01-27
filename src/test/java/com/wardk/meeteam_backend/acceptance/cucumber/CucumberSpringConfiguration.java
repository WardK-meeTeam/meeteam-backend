package com.wardk.meeteam_backend.acceptance.cucumber;

import com.wardk.meeteam_backend.acceptance.common.DatabaseCleaner;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Cucumber Spring 통합 설정
 * 모든 Cucumber 시나리오에서 Spring Context를 공유합니다.
 */
@CucumberContextConfiguration
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Before
    public void setUp() {
        RestAssured.port = port;
        databaseCleaner.execute();
    }

}
