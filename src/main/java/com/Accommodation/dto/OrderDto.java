package com.Accommodation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * OrderDto (주문 요청용)
 *
 * 화면에서 "어떤 숙소를, 몇 박" 주문할지 전달하는 객체입니다.
 * Controller → Service로 전달됩니다.
 */
@Getter
@Setter
public class OrderDto {

    @NotNull(message = "숙소 ID는 필수입니다.")
    private Long accomId;   // 주문할 숙소 ID

    @Min(value = 1, message = "최소 1박 이상 주문해야 합니다.")
    @Max(value = 30, message = "최대 30박까지 주문 가능합니다.")
    private int count;      // 주문 수량 (박수)
}
