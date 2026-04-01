package com.Accommodation.service;

import com.Accommodation.dto.MemberFormDto;
import com.Accommodation.entity.Member;
import com.Accommodation.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🧾 MemberService (회원 서비스)
 *
 * ▶ 실제 비즈니스 로직(핵심 처리)을 담당하는 클래스입니다.
 *
 * 역할:
 * - 회원가입 처리
 * - 이메일 중복 체크
 * - Entity 생성 및 DB 저장
 *
 * 흐름:
 * Controller → Service → Repository → DB
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    /**
     * 📦 MemberRepository 주입
     */
    private final MemberRepository memberRepository;

    /**
     * 🔐 비밀번호 암호화 객체 주입
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 🛠 회원가입 처리 메서드
     *
     * 전체 흐름:
     * 1. 이메일 중복 체크
     * 2. DTO → Entity 변환
     * 3. DB 저장
     */
    public Member saveMember(MemberFormDto memberFormDto) {

        // 1️⃣ 이메일 중복 체크
        validateDuplicateMember(memberFormDto.getEmail());

        // 2️⃣ DTO → Entity 변환 + 비밀번호 암호화
        Member member = Member.createMember(memberFormDto, passwordEncoder);

        // 3️⃣ DB 저장
        return memberRepository.save(member);
    }

    /**
     * 🔍 이메일 중복 체크
     *
     * ▶ 이미 존재하는 이메일이면 예외 발생
     */
    private void validateDuplicateMember(String email) {

        Member findMember = memberRepository.findByEmail(email);

        if (findMember != null) {
            throw new IllegalStateException("이미 가입된 회원입니다.");
        }
    }
}