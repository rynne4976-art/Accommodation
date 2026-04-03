package com.Accommodation.service;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.constant.OrderStatus;
import com.Accommodation.dto.OrderDto;
import com.Accommodation.dto.OrderHistDto;
import com.Accommodation.dto.OrderItemDto;
import com.Accommodation.dto.OrderUpdateDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.entity.AccomOperationDay;
import com.Accommodation.entity.AccomOperationPolicy;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Order;
import com.Accommodation.entity.OrderItem;
import com.Accommodation.entity.OrderStayDate;
import com.Accommodation.exception.OutOfStockException;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.OrderItemRepository;
import com.Accommodation.repository.OrderRepository;
import com.Accommodation.repository.OrderStayDateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final AccomRepository accomRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStayDateRepository orderStayDateRepository;

    // ── 주문 생성 ────────────────────────────────────────────────────────────
    public Long order(OrderDto orderDto, String email) {

        Accom accom = accomRepository.findWithOperationInfoById(orderDto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);

        if (member == null) {
            throw new EntityNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        validateOrderRequest(
                accom,
                orderDto.getCheckInDate(),
                orderDto.getCheckOutDate(),
                orderDto.getGuestCount(),
                null
        );

        OrderItem orderItem = new OrderItem();
        orderItem.setAccom(accom);
        orderItem.setCount(orderDto.getCount());
        orderItem.setOrderPrice(accom.getPricePerNight());
        orderItem.setCheckInDate(orderDto.getCheckInDate());
        orderItem.setCheckOutDate(orderDto.getCheckOutDate());
        orderItem.setGuestCount(orderDto.getGuestCount());
        orderItem.setBookingStatus(BookingStatus.CONFIRMED);

        syncStayDates(orderItem, accom, orderDto.getCheckInDate(), orderDto.getCheckOutDate());

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

        order.cancelOrder();
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

        if (status == OrderStatus.ORDER) {
            for (OrderItem item : order.getOrderItems()) {
                item.setBookingStatus(BookingStatus.CONFIRMED);
            }
        }
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

        for (OrderItem item : order.getOrderItems()) {
            Accom accom = accomRepository.findWithOperationInfoById(item.getAccom().getId())
                    .orElseThrow(EntityNotFoundException::new);

            validateOrderRequest(
                    accom,
                    dto.getCheckInDate(),
                    dto.getCheckOutDate(),
                    dto.getGuestCount(),
                    item.getId()
            );

            item.setCheckInDate(dto.getCheckInDate());
            item.setCheckOutDate(dto.getCheckOutDate());
            item.setGuestCount(dto.getGuestCount());
            item.setCount((int) ChronoUnit.DAYS.between(dto.getCheckInDate(), dto.getCheckOutDate()));
            item.setBookingStatus(BookingStatus.CONFIRMED);

            syncStayDates(item, accom, dto.getCheckInDate(), dto.getCheckOutDate());
        }
    }

    // ── 예약 달력용 만실 날짜 조회 ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LocalDate> getSoldOutDates(Long accomId) {
        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        Integer roomCount = accom.getRoomCount();
        if (roomCount == null || roomCount <= 0) {
            return Collections.emptyList();
        }

        return orderStayDateRepository.findSoldOutDates(accomId, roomCount, OrderStatus.CANCEL);
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

    @Transactional(readOnly = true)
    public OrderHistDto getOrderDetail(Long orderId, String email) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (!order.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("주문 상세 조회 권한이 없습니다.");
        }

        OrderHistDto orderHistDto = new OrderHistDto(order);

        for (OrderItem orderItem : order.getOrderItems()) {
            String imgUrl = orderItem.getAccom().getAccomImgList().stream()
                    .filter(img -> "Y".equals(img.getRepImgYn()))
                    .map(AccomImg::getImgUrl)
                    .findFirst()
                    .orElse("");

            orderHistDto.addOrderItemDto(new OrderItemDto(orderItem, imgUrl));
        }

        return orderHistDto;
    }

    @Transactional(readOnly = true)
    public Long countOrder(String email) {
        return orderRepository.countOrder(email);
    }

    // ── 내부 공통 로직 ──────────────────────────────────────────────────────
    private void validateOrderRequest(Accom accom,
                                      LocalDate checkInDate,
                                      LocalDate checkOutDate,
                                      int guestCount,
                                      Long excludeOrderItemId) {

        if (!"Y".equals(accom.getReserveStatCd())) {
            throw new OutOfStockException("현재 예약이 불가능한 숙소입니다.");
        }

        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜를 입력해 주세요.");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("체크아웃 날짜는 체크인 날짜보다 이후여야 합니다.");
        }

        if (guestCount < 1) {
            throw new IllegalArgumentException("투숙 인원은 1명 이상이어야 합니다.");
        }

        if (guestCount > accom.getGuestCount()) {
            throw new IllegalArgumentException("투숙 인원이 최대 수용 인원(" + accom.getGuestCount() + "명)을 초과합니다.");
        }

        AccomOperationPolicy policy = accom.getOperationPolicy();
        if (policy == null) {
            throw new IllegalArgumentException("운영 정책이 등록되지 않은 숙소입니다.");
        }

        List<LocalDate> stayDates = buildStayDates(checkInDate, checkOutDate);

        if (stayDates.isEmpty()) {
            throw new IllegalArgumentException("숙박 날짜를 다시 선택해 주세요.");
        }

        if (checkInDate.isBefore(policy.getOperationStartDate())) {
            throw new IllegalArgumentException("운영 시작일 이전 날짜는 예약할 수 없습니다.");
        }

        LocalDate lastStayDate = stayDates.get(stayDates.size() - 1);
        if (lastStayDate.isAfter(policy.getOperationEndDate())) {
            throw new IllegalArgumentException("운영 종료일 이후 날짜는 예약할 수 없습니다.");
        }

        // 오늘 체크인인데 이미 체크인 시간이 지난 경우 예약 불가
        if (policy.getCheckInTime() != null) {
            LocalDate today = LocalDate.now();
            if (checkInDate.isEqual(today) && !LocalTime.now().isBefore(policy.getCheckInTime())) {
                throw new IllegalArgumentException("오늘은 체크인 시간이 지나 예약할 수 없습니다.");
            }
        }

        Set<LocalDate> operationDateSet = accom.getOperationDayList().stream()
                .map(AccomOperationDay::getOperationDate)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        for (LocalDate stayDate : stayDates) {
            if (!operationDateSet.contains(stayDate)) {
                throw new IllegalArgumentException("선택한 날짜 중 운영하지 않는 날짜가 포함되어 있습니다.");
            }

            long reservedCount = orderStayDateRepository.countReservedByDate(
                    accom.getId(),
                    stayDate,
                    OrderStatus.CANCEL,
                    excludeOrderItemId
            );

            if (reservedCount >= accom.getRoomCount()) {
                throw new IllegalArgumentException("선택한 날짜 중 예약이 마감된 날짜가 있습니다: " + stayDate);
            }

            LocalDate today = LocalDate.now();

            if (checkInDate.isBefore(today)) {
                throw new IllegalArgumentException("오늘 이전 날짜는 예약할 수 없습니다.");
            }
        }
    }

    private void syncStayDates(OrderItem orderItem,
                               Accom accom,
                               LocalDate checkInDate,
                               LocalDate checkOutDate) {

        orderItem.clearStayDates();

        for (LocalDate stayDate : buildStayDates(checkInDate, checkOutDate)) {
            OrderStayDate orderStayDate = new OrderStayDate();
            orderStayDate.setAccom(accom);
            orderStayDate.setStayDate(stayDate);
            orderItem.addStayDate(orderStayDate);
        }
    }

    private List<LocalDate> buildStayDates(LocalDate checkInDate, LocalDate checkOutDate) {
        List<LocalDate> stayDates = new ArrayList<>();

        LocalDate cursor = checkInDate;
        while (cursor.isBefore(checkOutDate)) {
            stayDates.add(cursor);
            cursor = cursor.plusDays(1);
        }

        return stayDates;
    }
}