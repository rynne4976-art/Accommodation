package com.Accommodation.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OrderDto {

    @NotNull(message = "숙소 ID는 필수입니다.")
    private Long accomId;

    @NotNull(message = "체크인 날짜는 필수입니다.")
    private LocalDate checkInDate;

    @NotNull(message = "체크아웃 날짜는 필수입니다.")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "투숙 인원은 1명 이상이어야 합니다.")
    private int guestCount = 1;

    // checkInDate ~ checkOutDate 로 계산
    public int getCount() {
        if (checkInDate == null || checkOutDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }
}
