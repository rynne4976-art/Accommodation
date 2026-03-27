package com.Accommodation.entity;

import com.Accommodation.constant.Role;
import com.Accommodation.dto.MemberFormDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 🧾 Member (회원 엔티티)
 *
 * ▶ 회원 정보를 DB에 저장하기 위한 클래스입니다.
 * ▶ JPA를 통해 테이블과 자동 매핑됩니다.
 *
 * 테이블명: member
 *
 * 주요 역할:
 * - 회원 정보 저장
 * - 회원가입 시 DTO → Entity 변환
 */
@Entity
@Table(name = "member")
@Getter
@Setter
@ToString
public class Member extends BaseTimeEntity {

    /**
     * 🔑 기본키 (회원 ID)
     * - DB에서 자동 증가
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    /**
     * 👤 회원 이름
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 📧 이메일 (로그인 ID 역할)
     * - 중복 불가
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 🔒 비밀번호
     * - 회원가입 시 암호화되어 저장됨
     */
    @Column(nullable = false)
    private String password;

    /**
     * 휴대폰 번호
     */
    @Column(nullable = false, length = 50)
    private String number;

    /**
     * 🏠 주소
     */
    @Column(nullable = false, length = 255)
    private String address;

    /**
     * 🔐 권한 (USER / ADMIN)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * 🛠 회원 생성 메서드
     *
     * ▶ DTO 데이터를 Entity로 변환합니다.
     * ▶ 회원가입 시 사용됩니다.
     *
     * 현재:
     * - PasswordEncoder를 사용하여 비밀번호를 암호화
     */
    public static Member createMember(MemberFormDto memberFormDto, PasswordEncoder passwordEncoder) {

        Member member = new Member();

        member.setName(memberFormDto.getName());
        member.setEmail(memberFormDto.getEmail());
        member.setAddress(memberFormDto.getAddress());

        // 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(memberFormDto.getPassword());
        member.setPassword(encodedPassword);

        // 기본 권한 USER
        member.setRole(Role.USER);

        return member;
    }
}