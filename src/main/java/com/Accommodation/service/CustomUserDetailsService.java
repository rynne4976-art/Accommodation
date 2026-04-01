package com.Accommodation.service;

import com.Accommodation.entity.Member;
import com.Accommodation.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 🧾 CustomUserDetailsService
 *
 * ▶ 로그인 시 사용자 정보를 DB에서 조회하는 클래스
 *
 * 역할:
 * - 이메일로 회원 조회
 * - Spring Security가 사용할 User 객체로 변환
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 🔐 로그인 시 실행되는 핵심 메서드
     *
     * ▶ 입력된 이메일로 DB 조회
     * ▶ UserDetails 객체로 변환
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Member member = memberRepository.findByEmail(email);

        if (member == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        return new User(
                member.getEmail(),
                member.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + member.getRole().name())
                )
        );
    }
}