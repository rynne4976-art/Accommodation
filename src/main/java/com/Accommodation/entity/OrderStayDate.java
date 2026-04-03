package com.Accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(
        name = "order_stay_date",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_stay_date_order_item_date",
                        columnNames = {"order_item_id", "stay_date"}
                )
        }
)
@Getter
@Setter
@ToString(exclude = {"orderItem", "accom"})
public class OrderStayDate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_stay_date_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accom_id", nullable = false)
    private Accom accom;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    public void setOrderItem(OrderItem orderItem) {
        if (this.orderItem != null) {
            this.orderItem.getStayDateList().remove(this);
        }

        this.orderItem = orderItem;

        if (orderItem != null && !orderItem.getStayDateList().contains(this)) {
            orderItem.getStayDateList().add(this);
        }
    }
}