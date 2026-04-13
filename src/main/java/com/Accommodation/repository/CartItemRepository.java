package com.Accommodation.repository;

import com.Accommodation.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** 특정 회원의 장바구니 목록 (등록 최신순) */
    List<CartItem> findByMemberEmailOrderByRegTimeDesc(String email);

    /** 특정 회원의 특정 장바구니 항목 (권한 검증용) */
    Optional<CartItem> findByIdAndMemberEmail(Long id, String email);

    void deleteByMemberEmail(String email);

    /**
     * 예약 확정 후 만실 알림 대상 조회
     * – 나(confirmerEmail) 외의 회원 중, 해당 숙소의 해당 날짜가 체크인~체크아웃 사이인 장바구니 항목
     */
    @Query("""
            SELECT c FROM CartItem c
            WHERE c.accom.id = :accomId
              AND c.member.email <> :confirmerEmail
              AND c.checkInDate <= :stayDate
              AND c.checkOutDate > :stayDate
            """)
    List<CartItem> findOtherUsersCartItemsByAccomAndDate(
            @Param("accomId") Long accomId,
            @Param("stayDate") LocalDate stayDate,
            @Param("confirmerEmail") String confirmerEmail);
}
