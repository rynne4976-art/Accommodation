package com.Accommodation.repository;

import com.Accommodation.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 회원의 주문 내역을 최신순으로 조회 (페이징)
    @Query("SELECT o FROM Order o WHERE o.member.email = :email ORDER BY o.orderDate DESC")
    List<Order> findOrders(@Param("email") String email, Pageable pageable);

    // 회원의 총 주문 수 (페이징 계산용)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.member.email = :email")
    Long countOrder(@Param("email") String email);
}
