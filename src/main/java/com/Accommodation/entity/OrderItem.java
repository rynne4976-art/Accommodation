package com.Accommodation.entity;

import com.Accommodation.constant.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_item")
@Getter
@Setter
@ToString(exclude = {"order", "accom", "stayDateList"})
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    // 어떤 주문에 속한 항목인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 어떤 숙소를 주문했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accom_id", nullable = false)
    private Accom accom;

    private int count;       // 박수
    private int orderPrice;  // 주문 당시 1박 기본 요금

    /** 1박 추가 요금 (인원 초과분 – 확정 시점 계산값) */
    private int surchargePerNight;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    /** 총 투숙 인원 (성인 + 아동) */
    private Integer guestCount;

    /** 성인 수 */
    private int adultCount;

    /** 아동 수 */
    private int childCount;

    @OneToMany(
            mappedBy = "orderItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("stayDate ASC")
    private List<OrderStayDate> stayDateList = new ArrayList<>();

    /** 총 결제 금액 = (기본 요금 + 추가 요금) × 박 수 */
    public int getTotalPrice() {
        return (orderPrice + surchargePerNight) * count;
    }

    /**
     * 예약 취소 (FSM 적용)
     * COMPLETED 상태에서 호출 시 BookingStatus.cancel()이 예외를 던짐
     */
    public void cancel() {
        this.bookingStatus = this.bookingStatus.cancel();
    }

    /**
     * 이용 완료 처리 (스케줄러 전용)
     * CONFIRMED 상태에서만 호출 가능
     */
    public void complete() {
        this.bookingStatus = this.bookingStatus.complete();
    }

    public void addStayDate(OrderStayDate stayDate) {
        if (stayDate == null) {
            return;
        }

        if (!this.stayDateList.contains(stayDate)) {
            this.stayDateList.add(stayDate);
        }

        if (stayDate.getOrderItem() != this) {
            stayDate.setOrderItem(this);
        }

        if (stayDate.getAccom() == null) {
            stayDate.setAccom(this.accom);
        }
    }

    public void clearStayDates() {
        List<OrderStayDate> copiedList = new ArrayList<>(this.stayDateList);
        for (OrderStayDate stayDate : copiedList) {
            stayDate.setOrderItem(null);
        }
    }
}