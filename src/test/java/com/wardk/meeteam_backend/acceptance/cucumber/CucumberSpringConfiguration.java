package com.wardk.meeteam_backend.acceptance.cucumber;

import com.wardk.meeteam_backend.acceptance.common.DatabaseCleaner;
import com.wardk.meeteam_backend.acceptance.cucumber.support.FakeS3Server;
import com.wardk.meeteam_backend.acceptance.cucumber.support.TestS3Config;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

/**
 * Cucumber Spring 통합 설정
 * 모든 Cucumber 시나리오에서 Spring Context를 공유합니다.
 */
@CucumberContextConfiguration
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3Config.class)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FakeS3Server fakeS3Server;

    @Before
    public void setUp() {
        RestAssured.port = port;
        databaseCleaner.execute();
        fakeS3Server.reset();

        // 초기 데이터 설정
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("data.sql"));
        populator.execute(dataSource);
    }

}
