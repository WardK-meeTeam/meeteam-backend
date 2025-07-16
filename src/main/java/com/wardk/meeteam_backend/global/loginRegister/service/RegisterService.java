package com.wardk.meeteam_backend.global.loginRegister.service;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.loginRegister.FileStore;
import com.wardk.meeteam_backend.global.loginRegister.UploadFile;
import com.wardk.meeteam_backend.global.loginRegister.dto.register.RegisterRequestDto;
import com.wardk.meeteam_backend.global.loginRegister.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final FileStore fileStore;


    @Transactional
    public String register(RegisterRequestDto registerRequestDto) throws IOException {

        memberRepository.findOptionByEmail(registerRequestDto.getEmail())
                .ifPresent(email -> {
                    throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
                });

        String storeFileName = null;
        MultipartFile file = registerRequestDto.getFile();
        if (file != null && !file.isEmpty()) {
            storeFileName = fileStore.storeFile(file).getStoreFileName();
        }

        Member member = Member.createMember(
                registerRequestDto.getEmail(),
                bCryptPasswordEncoder.encode(registerRequestDto.getPassword()),
                registerRequestDto.getName(),
                registerRequestDto.getJobType(),
                storeFileName,
                UserRole.USER
        );

        memberRepository.save(member);

        return registerRequestDto.getName();

    }


}
