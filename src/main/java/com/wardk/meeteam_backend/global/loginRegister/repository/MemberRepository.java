package com.wardk.meeteam_backend.global.loginRegister.repository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findOptionByEmail(String email);

    Member findByEmail(String email);

}
