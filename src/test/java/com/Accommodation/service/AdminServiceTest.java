package com.Accommodation.service;

import com.Accommodation.constant.Role;
import com.Accommodation.entity.Member;
import com.Accommodation.exception.AdminException;
import com.Accommodation.exception.ErrorCode;
import com.Accommodation.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.profiles.include=")
@ActiveProfiles("test")
@Transactional
class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("없는 회원 조회 시 AdminException(ADMIN_MEMBER_NOT_FOUND)을 던진다")
    void getMemberDetailThrowsAdminMemberNotFoundException() {
        AdminException exception = assertThrows(AdminException.class,
                () -> adminService.getMemberDetail(999L));

        assertEquals(ErrorCode.ADMIN_MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("현재 로그인한 관리자 자신의 ADMIN 권한은 해제할 수 없다")
    void updateMemberRoleThrowsAdminRoleDowngradeForbiddenException() {
        Member adminMember = createMember("admin@test.com", Role.ADMIN);
        memberRepository.save(adminMember);

        AdminException exception = assertThrows(AdminException.class,
                () -> adminService.updateMemberRole(adminMember.getId(), Role.USER, "admin@test.com"));

        assertEquals(ErrorCode.ADMIN_ROLE_DOWNGRADE_FORBIDDEN, exception.getErrorCode());
    }

    private Member createMember(String email, Role role) {
        Member member = new Member();
        member.setName("관리자");
        member.setEmail(email);
        member.setPassword("{bcrypt}test-password");
        member.setNumber("01012345678");
        member.setAddress("서울시 강남구");
        member.setRole(role);
        return member;
    }
}
