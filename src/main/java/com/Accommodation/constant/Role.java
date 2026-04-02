package com.Accommodation.constant;

/**
 * 🧾 Role (회원 권한)
 *
 * ▶ 회원의 권한을 구분하기 위한 Enum 클래스입니다.
 *
 * USER  : 일반 사용자
 * ADMIN : 관리자
 *
 * 왜 사용하는가?
 * - 문자열("USER", "ADMIN")로 관리하면 오타 위험이 있음
 * - Enum을 사용하면 안정성과 가독성이 좋아짐
 *
 * 예:
 *   member.setRole(Role.USER);
 */

public enum Role {
    USER,       // 일반 회원
    ADMIN       // 관리자
}