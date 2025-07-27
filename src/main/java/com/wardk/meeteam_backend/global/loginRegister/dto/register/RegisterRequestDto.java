package com.wardk.meeteam_backend.global.loginRegister.dto.register;


import com.wardk.meeteam_backend.domain.member.entity.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@ParameterObject
public class RegisterRequestDto {

    @Schema(description = "이름", example = "홍길동")
    @NotEmpty
    private String name;

    @Schema(description = "이메일", example = "meeteam@naver.com")
    @NotEmpty
    private String email;

    @Schema(description = "비밀번호", example = "qwer1234")
    @NotEmpty
    private String password;

    @Schema(description = "직무선택", example = "BACKEND")
    @NotNull
    private JobType jobType;


}

/*
{"name":"박희운" , "email" : "phu98@naver.com" , "password" : 1234 , "jobtype": WEB }


 */
