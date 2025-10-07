package com.wardk.meeteam_backend.test;

import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.auth.service.AuthService;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.auth.dto.login.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestController {
    private final AuthService authService;

    @DeleteMapping("/auth/delete")
    @Operation(summary = "테스트용 회원 탈퇴", description = "헤더에 토큰을 담으면, 해당 정보로 회원 탈퇴를 진행합니다.")
    public void deleteUserForTest(@AuthenticationPrincipal CustomSecurityUserDetails userDetails) {
        authService.deleteByEmail(userDetails.getUsername());
    }

    @Autowired
    DataSource dataSource;
    private List<String> list = new ArrayList<>();

    @GetMapping("/cpu")
    public String cpu() {
        log.info("cpu");
        long value = 0;
        for (long i = 0; i < 100000000000L; i++) {
            value++;
        }
        return "ok value=" + value;
    }



    @GetMapping("/jvm")
    public String jvm() {
        log.info("jvm");
        for (int i = 0; i < 1000000000L; i++) {
            list.add("hello jvm!" + i);
        }
        return "ok";
    }


    @GetMapping("/jdbc")
    public String jdbc() throws SQLException {
        log.info("jdbc");
        Connection conn = dataSource.getConnection();
        log.info("connection info={}", conn); //conn.close(); //커넥션을 닫지 않는다.
        return "ok";
    }


    @GetMapping("/error-log")
    public String errorLog() {
        log.error("error log");
        return "error";
    }

}
