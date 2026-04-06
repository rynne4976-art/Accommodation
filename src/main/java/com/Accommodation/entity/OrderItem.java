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
    private int orderPrice;  // 주문 당시 1박 가격

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    private Integer guestCount;

    @OneToMany(
            mappedBy = "orderItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("stayDate ASC")
    private List<OrderStayDate> stayDateList = new ArrayList<>();

    public int getTotalPrice() {
        return orderPrice * count;
    }

    public void cancel() {
        this.bookingStatus = BookingStatus.CANCELLED;
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