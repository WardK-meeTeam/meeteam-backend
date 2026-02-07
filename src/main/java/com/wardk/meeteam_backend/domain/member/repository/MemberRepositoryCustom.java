package com.wardk.meeteam_backend.domain.member.repository;

import com.wardk.meeteam_backend.domain.job.JobField;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {

    Page<Member> searchMembers(List<JobField> jobFields, List<String> skills, Pageable pageable);
}

