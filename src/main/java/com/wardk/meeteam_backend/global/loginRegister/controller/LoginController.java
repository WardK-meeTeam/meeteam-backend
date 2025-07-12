package com.wardk.meeteam_backend.global.loginRegister.controller;

import com.wardk.meeteam_backend.global.loginRegister.dto.login.LoginRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {


    @PostMapping("/api/login")
    public String login(@RequestBody LoginRequest request) {
        return request.getUsername();
    }

}
