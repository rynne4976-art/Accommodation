package com.Accommodation.repository;

import com.Accommodation.constant.OrderStatus;
import com.Accommodation.entity.OrderStayDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderStayDateRepository extends JpaRepository<OrderStayDate, Long> {

    @Query("""
            select count(osd.id)
            from OrderStayDate osd
            where osd.accom.id = :accomId
              and osd.stayDate = :stayDate
              and osd.orderItem.order.orderStatus <> :cancelStatus
              and (:excludeOrderItemId is null or osd.orderItem.id <> :excludeOrderItemId)
            """)
    long countReservedByDate(@Param("accomId") Long accomId,
                             @Param("stayDate") LocalDate stayDate,
                             @Param("cancelStatus") OrderStatus cancelStatus,
                             @Param("excludeOrderItemId") Long excludeOrderItemId);

    @Query("""
            select osd.stayDate
            from OrderStayDate osd
            where osd.accom.id = :accomId
              and osd.orderItem.order.orderStatus <> :cancelStatus
            group by osd.stayDate
            having count(osd.id) >= :roomCount
            order by osd.stayDate asc
            """)
    List<LocalDate> findSoldOutDates(@Param("accomId") Long accomId,
                                     @Param("roomCount") long roomCount,
                                     @Param("cancelStatus") OrderStatus cancelStatus);
}