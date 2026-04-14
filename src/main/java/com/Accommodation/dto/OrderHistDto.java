package com.Accommodation.dto;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.constant.OrderStatus;
import com.Accommodation.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderHistDto (주문 내역 표시용)
 *
 * 주문 내역 화면에서 주문 1건의 정보를 표시할 때 사용합니다.
 * 주문에 속한 상품 목록(orderItemDtos)을 함께 가집니다.
 */
@Getter
@Setter
public class OrderHistDto {

    private Long orderId;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private LocalDateTime cancelDate;
    private int totalPrice;
    private List<OrderItemDto> orderItemDtos = new ArrayList<>();

    // Order 엔티티 → DTO 변환
    public OrderHistDto(Order order) {
        this.orderId = order.getId();
        this.orderDate = order.getOrderDate();
        this.orderStatus = order.getOrderStatus();
        this.cancelDate = order.getOrderStatus() == OrderStatus.CANCEL ? order.getUpdateTime() : null;
        this.totalPrice = order.getTotalPrice();
    }

    public void addOrderItemDto(OrderItemDto orderItemDto) {
        orderItemDtos.add(orderItemDto);
    }

    /** 취소완료 / 이용완료 / 예약완료 */
    public String getDisplayStatus() {
        if (orderStatus == OrderStatus.CANCEL) {
            return "취소완료";
        }
        if (!orderItemDtos.isEmpty() &&
                orderItemDtos.stream().allMatch(i -> i.getBookingStatus() == BookingStatus.COMPLETED)) {
            return "이용완료";
        }
        return "예약완료";
    }
}
