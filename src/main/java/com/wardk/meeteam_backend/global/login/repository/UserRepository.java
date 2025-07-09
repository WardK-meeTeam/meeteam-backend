package com.wardk.meeteam_backend.global.login.repository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Member, Long> {

    Optional<Member> findOptionByUsername(String username);

    Member findByUsername(String membername);

}
