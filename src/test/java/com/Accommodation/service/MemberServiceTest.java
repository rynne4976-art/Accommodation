package com.Accommodation.service;

import com.Accommodation.dto.MemberFormDto;
import com.Accommodation.dto.PasswordChangeDto;
import com.Accommodation.entity.Member;
import com.Accommodation.exception.ErrorCode;
import com.Accommodation.exception.MemberException;
import com.Accommodation.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.profiles.include=")
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("중복 이메일 회원가입 시 MemberException(DUPLICATE_EMAIL)을 던진다")
    void saveMemberThrowsDuplicateEmailException() {
        memberService.saveMember(createMemberFormDto("dup@test.com", "Password123!"));

        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.saveMember(createMemberFormDto("dup@test.com", "Password123!")));

        assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 MemberException(INVALID_CURRENT_PASSWORD)을 던진다")
    void changePasswordThrowsInvalidCurrentPasswordException() {
        memberService.saveMember(createMemberFormDto("pwd@test.com", "Password123!"));

        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword("wrong-password");
        passwordChangeDto.setNewPassword("NewPassword123!");
        passwordChangeDto.setConfirmPassword("NewPassword123!");

        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.changePassword("pwd@test.com", passwordChangeDto));

        assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, exception.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호 변경 성공 시 새 비밀번호가 암호화되어 저장된다")
    void changePasswordUpdatesEncodedPassword() {
        memberService.saveMember(createMemberFormDto("success@test.com", "Password123!"));

        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword("Password123!");
        passwordChangeDto.setNewPassword("NewPassword123!");
        passwordChangeDto.setConfirmPassword("NewPassword123!");

        memberService.changePassword("success@test.com", passwordChangeDto);

        Member member = memberRepository.findByEmail("success@test.com");
        assertTrue(passwordEncoder.matches("NewPassword123!", member.getPassword()));
    }

    private MemberFormDto createMemberFormDto(String email, String password) {
        MemberFormDto memberFormDto = new MemberFormDto();
        memberFormDto.setName("테스트회원");
        memberFormDto.setEmail(email);
        memberFormDto.setPassword(password);
        memberFormDto.setAddress("서울시 강남구");
        memberFormDto.setDetailAddress("101호");
        memberFormDto.setPostcode("12345");
        memberFormDto.setNumber("01012345678");
        return memberFormDto;
    }
}
