package com.Accommodation.entity;

import com.Accommodation.constant.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "order_item")
@Getter
@Setter
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    // 어떤 주문에 속한 항목인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 어떤 숙소를 주문했는지 (B팀원의 Accom)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accom_id", nullable = false)
    private Accom accom;

    private int count;       // 주문 수량 (박수)
    private int orderPrice;  // 주문 당시 가격 (가격 변동 대비 스냅샷)

    private LocalDate checkInDate;   // 체크인 날짜 (리뷰 작성에 필요)
    private LocalDate checkOutDate;  // 체크아웃 날짜 (리뷰 작성에 필요)

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus; // 예약 상태 (리뷰 작성에 필요)

    private Integer guestCount; // 투숙객 인원수

    // 총 금액 = 가격 × 수량
    public int getTotalPrice() {
        return orderPrice * count;
    }

    // 주문 취소 시 재고 복구 등 후처리 (추후 구현)
    public void cancel() {
        // TODO: 재고/예약 가용일 복구 로직
    }
}
