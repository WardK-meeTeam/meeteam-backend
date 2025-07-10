package com.wardk.meeteam_backend.domain.member.entity;


import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Entity
@AllArgsConstructor
@Builder
@Getter
public class Member extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String email;

    private String password;

    private String nickName;

    private String realName;

    private String storeFileName;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    public Member() {
    }

    @Builder
    public Member(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Builder
    public Member(String email, String password, String nickName, JobType jobType , String storeFileName) {
        this.email = email;
        this.password = password;
        this.nickName = nickName;
        this.jobType = jobType;
        this.storeFileName = storeFileName;
    }

    public static Member createMember(String email, String password, String nickName, JobType jobType, String storeFileName) {
        return Member.builder()
                .email(email)
                .password(password)
                .nickName(nickName)
                .jobType(jobType)
                .storeFileName(storeFileName)
                .build();
    }
}
