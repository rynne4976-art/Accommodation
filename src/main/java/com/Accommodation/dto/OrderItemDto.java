package com.Accommodation.dto;

import com.Accommodation.entity.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OrderItemDto {

    private Long orderItemId;
    private String accomName;
    private int count;
    private int orderPrice;
    private String imgUrl;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;

    public OrderItemDto(OrderItem orderItem, String imgUrl) {
        this.orderItemId = orderItem.getId();
        this.accomName = orderItem.getAccom().getAccomName();
        this.count = orderItem.getCount();
        this.orderPrice = orderItem.getOrderPrice();
        this.imgUrl = imgUrl;
        this.checkInDate = orderItem.getCheckInDate();
        this.checkOutDate = orderItem.getCheckOutDate();
        this.guestCount = orderItem.getGuestCount();
    }
}
