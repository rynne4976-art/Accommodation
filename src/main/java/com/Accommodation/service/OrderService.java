package com.Accommodation.service;

import com.Accommodation.constant.OrderStatus;
import com.Accommodation.dto.OrderDto;
import com.Accommodation.dto.OrderHistDto;
import com.Accommodation.dto.OrderItemDto;
import com.Accommodation.entity.*;
import com.Accommodation.exception.OutOfStockException;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.OrderItemRepository;
import com.Accommodation.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final AccomRepository accomRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // 주문 생성
    public Long order(OrderDto orderDto, String email) {
        Accom accom = accomRepository.findById(orderDto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);

        // TODO: B팀이 Accom에 stockNumber 추가 후 아래 재고 확인 활성화
        // if (accom.getStockNumber() < orderDto.getCount()) {
        //     throw new OutOfStockException("재고가 부족합니다. 현재 재고: " + accom.getStockNumber());
        // }

        OrderItem orderItem = new OrderItem();
        orderItem.setAccom(accom);
        orderItem.setCount(orderDto.getCount());
        // TODO: B팀이 Accom.price를 Integer로 정상화 후 아래 활성화
        // orderItem.setOrderPrice(accom.getPrice());

        Order order = new Order();
        order.setMember(member);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.ORDER);
        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order);

        orderRepository.save(order);
        return order.getId();
    }

    // 주문 취소
    public void cancelOrder(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (!order.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("주문 취소 권한이 없습니다.");
        }

        order.cancelOrder();
        // TODO: B팀 Accom에 addStock() 추가 후 재고 복원 활성화
    }

    // 주문 내역 조회 (페이징)
    @Transactional(readOnly = true)
    public List<OrderHistDto> getOrderList(String email, Pageable pageable) {
        List<Order> orders = orderRepository.findOrders(email, pageable);
        List<OrderHistDto> orderHistDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderHistDto orderHistDto = new OrderHistDto(order);
            for (OrderItem orderItem : order.getOrderItems()) {
                // TODO: B팀 RoomImg 구현 후 실제 이미지 URL 연동
                OrderItemDto orderItemDto = new OrderItemDto(orderItem, "");
                orderHistDto.addOrderItemDto(orderItemDto);
            }
            orderHistDtos.add(orderHistDto);
        }
        return orderHistDtos;
    }

    // 총 주문 수 (페이징 계산용)
    @Transactional(readOnly = true)
    public Long countOrder(String email) {
        return orderRepository.countOrder(email);
    }
}
