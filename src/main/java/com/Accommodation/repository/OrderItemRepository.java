package com.Accommodation.repository;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByOrderMemberIdAndAccomIdAndCheckOutDateLessThanEqualAndBookingStatusNot(
            Long memberId,
            Long accomId,
            LocalDate checkOutDate,
            BookingStatus bookingStatus
    );
}
