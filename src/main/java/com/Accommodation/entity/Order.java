package com.Accommodation.entity;

import com.Accommodation.constant.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    // 어떤 회원의 주문인지 (회원 1명이 여러 주문 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    // 주문 1건에 여러 상품 가능
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * 주문 취소 (FSM 적용)
     *
     * 1) OrderStatus FSM 검증 → CANCEL 종단에서 재취소 시도 차단
     * 2) COMPLETED 항목 포함 시 취소 불가 (이용 완료 후 취소 차단)
     * 3) 모든 OrderItem을 원자적으로 CANCELLED 처리
     */
    public void cancelOrder() {
        // COMPLETED 항목 포함 여부 먼저 검증 (상태 변경 전에 체크)
        boolean hasCompleted = orderItems.stream()
                .anyMatch(item -> !item.getBookingStatus().canCancel());
        if (hasCompleted) {
            throw new IllegalStateException("이미 이용 완료된 예약이 포함되어 있어 취소할 수 없습니다.");
        }

        // OrderStatus FSM: ORDER.cancel() → CANCEL, CANCEL.cancel() → 예외
        this.orderStatus = this.orderStatus.cancel();

        // 모든 OrderItem 원자적 취소
        for (OrderItem orderItem : orderItems) {
            orderItem.cancel();
        }
    }

    // 총 주문 금액
    public int getTotalPrice() {
        return orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }
}
