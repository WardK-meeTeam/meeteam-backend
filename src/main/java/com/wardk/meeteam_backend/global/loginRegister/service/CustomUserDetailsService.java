package com.wardk.meeteam_backend.global.loginRegister.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.loginRegister.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final MemberRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

    // DB에서 조회
    Member member = userRepository.findByEmail(email);

    if (member != null) {

      // UserDetails에 담아서 return 하면 AuthenticationManager가 검증함
      return new CustomSecurityUserDetails(member);
    }

    return null;
  }
}
