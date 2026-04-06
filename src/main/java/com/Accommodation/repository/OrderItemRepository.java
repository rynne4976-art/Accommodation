package com.Accommodation.repository;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByOrderMemberIdAndAccomIdAndCheckOutDateLessThanEqualAndBookingStatusNot(
            Long memberId,
            Long accomId,
            LocalDate checkOutDate,
            BookingStatus bookingStatus
    );

    /**
     * 스케줄러 전용: CONFIRMED 상태이면서 체크아웃 날짜가 기준일 이하인 OrderItem 조회
     * → 매일 자동 COMPLETED 처리에 사용
     */
    @Query("""
            SELECT oi FROM OrderItem oi
            WHERE oi.bookingStatus = :status
              AND oi.checkOutDate <= :baseDate
            """)
    List<OrderItem> findAllByBookingStatusAndCheckOutDateBefore(
            @Param("status") BookingStatus status,
            @Param("baseDate") LocalDate baseDate
    );

    /**
     * 날짜별 CONFIRMED 예약 수 조회 (만실 카운트용)
     * OrderStayDateRepository 와 용도가 다름:
     * 이 쿼리는 체크인~체크아웃 범위로 겹치는 CONFIRMED 예약 수를 반환한다.
     */
    @Query("""
            SELECT COUNT(oi) FROM OrderItem oi
            WHERE oi.accom.id = :accomId
              AND oi.bookingStatus = com.Accommodation.constant.BookingStatus.CONFIRMED
              AND oi.checkInDate <= :targetDate
              AND oi.checkOutDate > :targetDate
              AND (:excludeOrderItemId IS NULL OR oi.id <> :excludeOrderItemId)
            """)
    long countConfirmedByDate(
            @Param("accomId") Long accomId,
            @Param("targetDate") LocalDate targetDate,
            @Param("excludeOrderItemId") Long excludeOrderItemId
    );
}
