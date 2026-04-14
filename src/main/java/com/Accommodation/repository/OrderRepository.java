package com.Accommodation.repository;

import com.Accommodation.constant.OrderStatus;
import com.Accommodation.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    // 회원의 주문 내역을 최신순으로 조회 (페이징)
    @Query("SELECT o FROM Order o WHERE o.member.email = :email ORDER BY o.orderDate DESC")
    List<Order> findOrders(@Param("email") String email, Pageable pageable);

    // 회원의 주문 내역을 상태별로 최신순 조회 (페이징)
    @Query("SELECT o FROM Order o WHERE o.member.email = :email AND o.orderStatus = :status ORDER BY o.orderDate DESC")
    List<Order> findOrdersByStatus(@Param("email") String email, @Param("status") OrderStatus status, Pageable pageable);

    // 기간 범위 내 주문 전체 조회 — orderItems, accom JOIN FETCH (N+1 방지)
    // accomImgList 는 트랜잭션 내 지연 로딩으로 처리 (두 컬렉션 동시 FETCH 시 MultipleBagFetchException)
    @Query("""
            SELECT DISTINCT o FROM Order o
            JOIN FETCH o.orderItems oi
            JOIN FETCH oi.accom a
            WHERE o.member.email = :email
              AND o.orderDate >= :from
              AND o.orderDate <= :to
            ORDER BY o.orderDate DESC
            """)
    List<Order> findOrdersByDateRange(@Param("email") String email,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    // 회원의 총 주문 수 (페이징 계산용)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.member.email = :email")
    Long countOrder(@Param("email") String email);
}
