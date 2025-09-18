package com.wardk.meeteam_backend.domain.member.repository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findOptionByEmail(String email);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);
}
