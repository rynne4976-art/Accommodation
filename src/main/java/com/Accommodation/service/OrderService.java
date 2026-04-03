package com.Accommodation.service;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.constant.OrderStatus;
import com.Accommodation.dto.OrderDto;
import com.Accommodation.dto.OrderHistDto;
import com.Accommodation.dto.OrderItemDto;
import com.Accommodation.dto.OrderUpdateDto;
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

    // ── 주문 생성 ────────────────────────────────────────────────────────────
    public Long order(OrderDto orderDto, String email) {

        Accom accom = accomRepository.findById(orderDto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);

        // 예약 가능 상태 확인
        if (!"Y".equals(accom.getReserveStatCd())) {
            throw new OutOfStockException("현재 예약이 불가능한 숙소입니다.");
        }

        // 체크인/체크아웃 날짜 유효성 검증
        if (orderDto.getCheckInDate() == null || orderDto.getCheckOutDate() == null) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜를 입력해 주세요.");
        }
        if (!orderDto.getCheckOutDate().isAfter(orderDto.getCheckInDate())) {
            throw new IllegalArgumentException("체크아웃 날짜는 체크인 날짜보다 이후여야 합니다.");
        }

        // 투숙 인원 검증
        if (orderDto.getGuestCount() > accom.getGuestCount()) {
            throw new IllegalArgumentException("투숙 인원이 최대 수용 인원(" + accom.getGuestCount() + "명)을 초과합니다.");
        }

        // 주문 상품 생성
        OrderItem orderItem = new OrderItem();
        orderItem.setAccom(accom);
        orderItem.setCount(orderDto.getCount());
        orderItem.setOrderPrice(accom.getPricePerNight());
        orderItem.setCheckInDate(orderDto.getCheckInDate());
        orderItem.setCheckOutDate(orderDto.getCheckOutDate());
        orderItem.setGuestCount(orderDto.getGuestCount());
        orderItem.setBookingStatus(BookingStatus.CONFIRMED);

        // 객실 수 감소
        accom.decreaseRoomCount();

        // 주문 생성
        Order order = new Order();
        order.setMember(member);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.ORDER);
        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order);

        orderRepository.save(order);
        return order.getId();
    }

    // ── 주문 취소 ────────────────────────────────────────────────────────────
    public void cancelOrder(Long orderId, String email) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (!order.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("주문 취소 권한이 없습니다.");
        }

        order.cancelOrder(); // OrderStatus → CANCEL, 각 OrderItem.cancel() → roomCount 복구
    }

    // ── 관리자 예약 상태 변경 ──────────────────────────────────────────────
    public void updateOrderStatusByAdmin(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (order.getOrderStatus() == status) {
            return;
        }

        if (order.getOrderStatus() == OrderStatus.CANCEL && status == OrderStatus.ORDER) {
            throw new IllegalArgumentException("취소된 예약은 다시 예약 완료 상태로 변경할 수 없습니다.");
        }

        if (status == OrderStatus.CANCEL) {
            order.cancelOrder();
            return;
        }

        order.setOrderStatus(status);
    }

    // ── 주문 수정 (체크인/체크아웃/인원 변경) ────────────────────────────────
    public void updateOrder(Long orderId, OrderUpdateDto dto, String email) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (!order.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        if (order.getOrderStatus() == OrderStatus.CANCEL) {
            throw new IllegalStateException("취소된 주문은 수정할 수 없습니다.");
        }
        if (!dto.getCheckOutDate().isAfter(dto.getCheckInDate())) {
            throw new IllegalArgumentException("체크아웃 날짜는 체크인 날짜보다 이후여야 합니다.");
        }

        for (OrderItem item : order.getOrderItems()) {
            int maxGuest = item.getAccom().getGuestCount();
            if (dto.getGuestCount() > maxGuest) {
                throw new IllegalArgumentException("투숙 인원이 최대 수용 인원(" + maxGuest + "명)을 초과합니다.");
            }
            item.setCheckInDate(dto.getCheckInDate());
            item.setCheckOutDate(dto.getCheckOutDate());
            item.setGuestCount(dto.getGuestCount());
            item.setCount((int) java.time.temporal.ChronoUnit.DAYS.between(
                    dto.getCheckInDate(), dto.getCheckOutDate()));
        }
    }

    // ── 주문 내역 조회 (페이징) ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OrderHistDto> getOrderList(String email, Pageable pageable) {

        List<Order> orders = orderRepository.findOrders(email, pageable);
        List<OrderHistDto> orderHistDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderHistDto orderHistDto = new OrderHistDto(order);

            for (OrderItem orderItem : order.getOrderItems()) {
                String imgUrl = orderItem.getAccom().getAccomImgList().stream()
                        .filter(img -> "Y".equals(img.getRepImgYn()))
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
