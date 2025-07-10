package com.wardk.meeteam_backend.global.loginRegister.dto.register;


import com.wardk.meeteam_backend.domain.member.entity.JobType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
public class RegisterRequestDto {

    @NotEmpty
    private String name;

    @NotEmpty
    private String email;

    @NotEmpty
    private String password;

    @NotNull
    private JobType jobType;

    private MultipartFile file;
}

/*
{"name":"박희운" , "email" : "phu98@naver.com" , "password" : 1234 , "jobtype": WEB }


 */
