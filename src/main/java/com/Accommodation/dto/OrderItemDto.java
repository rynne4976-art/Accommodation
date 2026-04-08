package com.Accommodation.dto;

import com.Accommodation.constant.BookingStatus;
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
    private int surchargePerNight;
    private String imgUrl;
    private String accomDetail;
    private String gradeLabel;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private int adultCount;
    private int childCount;
    private int roomCount;
    private BookingStatus bookingStatus;

    public OrderItemDto(OrderItem orderItem, String imgUrl) {
        this.orderItemId       = orderItem.getId();
        this.accomName         = orderItem.getAccom().getAccomName();
        this.count             = orderItem.getCount();
        this.orderPrice        = orderItem.getOrderPrice();
        this.surchargePerNight = orderItem.getSurchargePerNight();
        this.imgUrl            = imgUrl;
        this.accomDetail       = orderItem.getAccom().getAccomDetail();
        this.gradeLabel        = toGradeLabel(orderItem);
        this.checkInDate       = orderItem.getCheckInDate();
        this.checkOutDate      = orderItem.getCheckOutDate();
        this.guestCount        = orderItem.getGuestCount();
        this.adultCount        = orderItem.getAdultCount();
        this.childCount        = orderItem.getChildCount();
        this.roomCount         = orderItem.getRoomCount();
        this.bookingStatus     = orderItem.getBookingStatus();
    }

    /** 총 결제 금액 = (기본 + 추가) × 박 수 × 객실 수 */
    public int getTotalPrice() {
        return (orderPrice + surchargePerNight) * count * roomCount;
    }

    private String toGradeLabel(OrderItem orderItem) {
        if (orderItem.getAccom().getGrade() == null) {
            return "";
        }
        return switch (orderItem.getAccom().getGrade()) {
            case ONE   -> "1성급";
            case TWO   -> "2성급";
            case THREE -> "3성급";
            case FOUR  -> "4성급";
            case FIVE  -> "5성급";
        };
    }
}
