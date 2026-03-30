package com.Accommodation.dto;

import com.Accommodation.entity.OrderItem;
import lombok.Getter;
import lombok.Setter;

/**
 * OrderItemDto (주문 상품 정보용)
 *
 * 주문 내역 화면에서 "어떤 숙소를, 얼마에, 몇 박" 표시할 때 사용합니다.
 * OrderHistDto 안에 리스트로 포함됩니다.
 */
@Getter
@Setter
public class OrderItemDto {

    private String accomNm;    // 숙소명
    private int count;         // 주문 수량 (박수)
    private int orderPrice;    // 주문 당시 가격
    private String imgUrl;     // 숙소 대표 이미지 경로

    // OrderItem 엔티티 → DTO 변환
    public OrderItemDto(OrderItem orderItem, String imgUrl) {
        this.accomNm = orderItem.getAccom().getAccomDetail();
        this.count = orderItem.getCount();
        this.orderPrice = orderItem.getOrderPrice();
        this.imgUrl = imgUrl;
    }
}
