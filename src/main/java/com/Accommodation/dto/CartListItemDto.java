package com.Accommodation.dto;

import com.Accommodation.constant.AccomType;
import com.Accommodation.entity.CartItem;
import com.Accommodation.util.GuestPricingUtils;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 장바구니 목록 표시용 DTO */
@Getter
public class CartListItemDto {

    private final Long cartItemId;
    private final Long accomId;
    private final String accomName;
    private final AccomType accomType;
    private final String accomTypeName;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    /** 박 수 */
    private final int nights;

    /** 성인 수 */
    private final int adultCount;

    /** 아동 수 */
    private final int childCount;

    /** 객실 수 */
    private final int roomCount;

    /** 1박 기본 요금 */
    private final int pricePerNight;

    /** 1박 추가 요금 (인원 초과분) */
    private final int surchargePerNight;

    /** 총 결제 예상 금액 = (기본 + 추가) × 박 수 × 객실 수 */
    private final int totalPrice;

    private final boolean reservable;
    private final String unavailableReason;

    private final String repImgUrl;

    public CartListItemDto(CartItem cartItem, String repImgUrl) {
        this(cartItem, repImgUrl, null);
    }

    public CartListItemDto(CartItem cartItem, String repImgUrl, String unavailableReason) {
        this.cartItemId    = cartItem.getId();
        this.accomId       = cartItem.getAccom().getId();
        this.accomName     = cartItem.getAccom().getAccomName();
        this.accomType     = cartItem.getAccom().getAccomType();
        this.accomTypeName = GuestPricingUtils.getTypeName(cartItem.getAccom().getAccomType());
        this.checkInDate   = cartItem.getCheckInDate();
        this.checkOutDate  = cartItem.getCheckOutDate();
        this.nights        = (int) ChronoUnit.DAYS.between(cartItem.getCheckInDate(), cartItem.getCheckOutDate());
        this.adultCount    = cartItem.getAdultCount();
        this.childCount    = cartItem.getChildCount();
        this.roomCount     = cartItem.getRoomCount();
        this.pricePerNight = cartItem.getAccom().getPricePerNight();
        this.surchargePerNight = GuestPricingUtils.calculateSurchargePerNight(
                cartItem.getAccom().getAccomType(),
                cartItem.getAdultCount(),
                cartItem.getChildCount(),
                cartItem.getAccom().getPricePerNight()
        );
        this.totalPrice = (this.pricePerNight + this.surchargePerNight) * this.nights * this.roomCount;
        this.unavailableReason = unavailableReason;
        this.reservable = unavailableReason == null;
        this.repImgUrl  = repImgUrl;
    }
}
