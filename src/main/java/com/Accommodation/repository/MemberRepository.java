package com.Accommodation.repository;

import com.Accommodation.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 Repository 인터페이스
 *
 * JpaRepository<Member, Long>을 상속합니다:
 *   - Member: 이 Repository가 관리하는 엔티티 타입
 *   - Long: Member 엔티티의 기본키(PK) 타입
 *
 * JpaRepository를 상속하면 기본 CRUD 메소드가 자동 제공됩니다:
 *   - save(): 저장 또는 수정
 *   - findById(): ID로 조회
 *   - findAll(): 전체 조회
 *   - delete(): 삭제
 *
 * 커스텀(쿼리) 메소드:
 *   - findByEmail(): 이메일로 회원 조회
 *     Spring Data JPA가 메소드 이름을 파싱하여 쿼리를 자동 생성합니다
 *     findByEmail → SELECT * FROM member WHERE email = ?
 */

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 이메일로 회원을 조회하는 메소드
    // 로그인 시 이메일로 회원을 찾아야 하므로 필수!
    // 자동 생성 쿼리: SELECT m FROM Member m WHERE m.email = :email
    Member findByEmail(String email);
}