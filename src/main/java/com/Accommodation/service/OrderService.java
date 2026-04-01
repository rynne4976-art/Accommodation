package com.Accommodation.service;

import com.Accommodation.constant.OrderStatus;
import com.Accommodation.dto.OrderDto;
import com.Accommodation.dto.OrderHistDto;
import com.Accommodation.dto.OrderItemDto;
import com.Accommodation.entity.*;
import com.Accommodation.exception.OutOfStockException;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.MemberRepository;
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

    // ── 주문 생성 ────────────────────────────────────────────────────────────
    public Long order(OrderDto orderDto, String email) {

        Accom accom = accomRepository.findById(orderDto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);

        // 예약 가능 상태 확인 (reserveStatCd != "Y" 이면 예약 불가)
        if (!"Y".equals(accom.getReserveStatCd())) {
            throw new OutOfStockException("현재 예약이 불가능한 숙소입니다.");
        }

        // 주문 상품 생성
        OrderItem orderItem = new OrderItem();
        orderItem.setAccom(accom);
        orderItem.setCount(orderDto.getCount());
        orderItem.setOrderPrice(accom.getPrice()); // price 가 Integer 로 정상화됨

        // 주문 생성
        Order order = new Order();
        order.setMember(member);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.ORDER);
        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order);

        orderRepository.save(order); // cascade = ALL → orderItem 도 함께 저장
        return order.getId();
    }

    // ── 주문 취소 ────────────────────────────────────────────────────────────
    public void cancelOrder(Long orderId, String email) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (!order.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("주문 취소 권한이 없습니다.");
        }

        order.cancelOrder(); // OrderStatus → CANCEL, OrderItem.cancel() 호출
    }

    // ── 주문 내역 조회 (페이징) ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OrderHistDto> getOrderList(String email, Pageable pageable) {

        List<Order> orders = orderRepository.findOrders(email, pageable);
        List<OrderHistDto> orderHistDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderHistDto orderHistDto = new OrderHistDto(order);

            for (OrderItem orderItem : order.getOrderItems()) {
                // 대표 이미지(repimgYn = "Y") URL 조회
                String imgUrl = orderItem.getAccom().getAccomImgList().stream()
                        .filter(img -> "Y".equals(img.getRepimgYn()))
                        .map(AccomImg::getImgUrl)
                        .findFirst()
                        .orElse("");

                orderHistDto.addOrderItemDto(new OrderItemDto(orderItem, imgUrl));
            }
            orderHistDtos.add(orderHistDto);
        }
        return orderHistDtos;
    }

    // ── 총 주문 수 (페이징 계산용) ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public Long countOrder(String email) {
        return orderRepository.countOrder(email);
    }
}
