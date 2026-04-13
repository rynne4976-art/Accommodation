package com.Accommodation.service;

import com.Accommodation.constant.SocialMemberDefaults;
import com.Accommodation.dto.MemberFormDto;
import com.Accommodation.dto.MemberUpdateDto;
import com.Accommodation.dto.PasswordChangeDto;
import com.Accommodation.constant.Role;
import com.Accommodation.entity.Member;
import com.Accommodation.exception.ErrorCode;
import com.Accommodation.exception.MemberException;
import com.Accommodation.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

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

    @Transactional(readOnly = true)
    public Member getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email);

        if (member == null) {
            throw new MemberException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return member;
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return StringUtils.hasText(email) && memberRepository.findByEmail(email) != null;
    }

    public void updateMember(String email, MemberUpdateDto memberUpdateDto) {
        Member member = getMemberByEmail(email);

        if (!member.isSocialMember()) {
            String currentPassword = memberUpdateDto.getCurrentPassword();
            if (!StringUtils.hasText(currentPassword)
                    || !passwordEncoder.matches(currentPassword, member.getPassword())) {
                throw new MemberException(ErrorCode.INVALID_CURRENT_PASSWORD);
            }
        }

        member.updateProfile(memberUpdateDto);
    }

    public void changePassword(String email, PasswordChangeDto passwordChangeDto) {
        Member member = getMemberByEmail(email);

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), member.getPassword())) {
            throw new MemberException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        if (passwordEncoder.matches(passwordChangeDto.getNewPassword(), member.getPassword())) {
            throw new MemberException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        member.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
    }

    public Member upsertSocialMember(String provider, String email, String name, String providerId) {
        Member member = memberRepository.findByEmail(email);

        if (member == null) {
            member = new Member();
            member.setName(StringUtils.hasText(name) ? name : "소셜 사용자");
            member.setEmail(email);
            member.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            member.setNumber(SocialMemberDefaults.DEFAULT_NUMBER);
            member.setAddress(SocialMemberDefaults.DEFAULT_ADDRESS);
            member.setRole(Role.USER);
        }

        member.linkSocialAccount(provider, providerId);
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredReservationInfo(String email) {
        Member member = getMemberByEmail(email);
        return hasRequiredReservationInfo(member);
    }

    private boolean hasRequiredReservationInfo(Member member) {
        return StringUtils.hasText(member.getNumber())
                && !SocialMemberDefaults.DEFAULT_NUMBER.equals(member.getNumber())
                && StringUtils.hasText(member.getAddress())
                && !SocialMemberDefaults.DEFAULT_ADDRESS.equals(member.getAddress());
    }

    /**
     * 🔍 이메일 중복 체크
     *
     * ▶ 이미 존재하는 이메일이면 예외 발생
     */
    private void validateDuplicateMember(String email) {

        Member findMember = memberRepository.findByEmail(email);

        if (findMember != null) {
            throw new MemberException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}
