package com.Accommodation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 장바구니 추가 요청 DTO */
@Getter
@Setter
public class CartItemDto {

    @NotNull(message = "숙소 ID는 필수입니다.")
    private Long accomId;

    @NotNull(message = "체크인 날짜는 필수입니다.")
    private LocalDate checkInDate;

    @NotNull(message = "체크아웃 날짜는 필수입니다.")
    private LocalDate checkOutDate;

    /** 성인 수 (최소 1명) */
    @Min(value = 1, message = "성인은 최소 1명 이상이어야 합니다.")
    private int adultCount = 1;

    /** 아동 수 (0명 이상) */
    @Min(value = 0, message = "아동 수는 0명 이상이어야 합니다.")
    private int childCount = 0;
}
