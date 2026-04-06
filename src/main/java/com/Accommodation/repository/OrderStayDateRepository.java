package com.Accommodation.repository;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.entity.OrderStayDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderStayDateRepository extends JpaRepository<OrderStayDate, Long> {

    /**
     * 날짜별 CONFIRMED 예약 수 조회
     *
     * CANCELLED / COMPLETED 는 제외하고 오직 CONFIRMED 만 카운트한다.
     * → "확정된 것만 진짜 점유"라는 비즈니스 규칙 반영
     *
     * @param excludeOrderItemId 수정 시 본인 항목을 카운트에서 제외 (null 이면 제외 없음)
     */
    @Query("""
            SELECT COUNT(osd.id)
            FROM OrderStayDate osd
            WHERE osd.accom.id = :accomId
              AND osd.stayDate = :stayDate
              AND osd.orderItem.bookingStatus = :confirmedStatus
              AND (:excludeOrderItemId IS NULL OR osd.orderItem.id <> :excludeOrderItemId)
            """)
    long countConfirmedByDate(@Param("accomId") Long accomId,
                              @Param("stayDate") LocalDate stayDate,
                              @Param("confirmedStatus") BookingStatus confirmedStatus,
                              @Param("excludeOrderItemId") Long excludeOrderItemId);

    /**
     * 만실 날짜 목록 조회 (달력 표시용)
     *
     * CONFIRMED 예약 수가 객실 수 이상인 날짜만 반환한다.
     */
    @Query("""
            SELECT osd.stayDate
            FROM OrderStayDate osd
            WHERE osd.accom.id = :accomId
              AND osd.orderItem.bookingStatus = :confirmedStatus
            GROUP BY osd.stayDate
            HAVING COUNT(osd.id) >= :roomCount
            ORDER BY osd.stayDate ASC
            """)
    List<LocalDate> findSoldOutDates(@Param("accomId") Long accomId,
                                     @Param("roomCount") long roomCount,
                                     @Param("confirmedStatus") BookingStatus confirmedStatus);
}
