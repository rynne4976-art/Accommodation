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
import com.Accommodation.entity.CartItem;
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
import com.Accommodation.util.GuestPricingUtils;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    // ── 장바구니 → 예약 확정 (CartService 에서 호출) ─────────────────────────
    public Long createOrderFromCartItem(CartItem cartItem, String email) {

        Accom accom = accomRepository.findWithOperationInfoById(cartItem.getAccom().getId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new EntityNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        int adultCount = cartItem.getAdultCount();
        int childCount = cartItem.getChildCount();
        int surcharge  = GuestPricingUtils.calculateSurchargePerNight(
                accom.getAccomType(), adultCount, childCount, accom.getPricePerNight());
        int nights = (int) ChronoUnit.DAYS.between(cartItem.getCheckInDate(), cartItem.getCheckOutDate());

        OrderItem orderItem = new OrderItem();
        orderItem.setAccom(accom);
        orderItem.setCount(nights);
        orderItem.setOrderPrice(accom.getPricePerNight());
        orderItem.setSurchargePerNight(surcharge);
        orderItem.setAdultCount(adultCount);
        orderItem.setChildCount(childCount);
        orderItem.setGuestCount(adultCount + childCount);
        orderItem.setCheckInDate(cartItem.getCheckInDate());
        orderItem.setCheckOutDate(cartItem.getCheckOutDate());
        orderItem.setBookingStatus(BookingStatus.CONFIRMED);   // 확정만 점유 반영

        syncStayDates(orderItem, accom, cartItem.getCheckInDate(), cartItem.getCheckOutDate());

        Order order = new Order();
        order.setMember(member);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.ORDER);
        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order);

        orderRepository.save(order);
        return order.getId();
    }

    // ── 직접 예약 (레거시 – 관리자/테스트용) ────────────────────────────────
    public Long order(OrderDto orderDto, String email) {

        Accom accom = accomRepository.findWithOperationInfoById(orderDto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new EntityNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        int adultCount = orderDto.getAdultCount();
        int childCount = orderDto.getChildCount();

        GuestPricingUtils.validateGuestCount(accom.getAccomType(), adultCount, childCount);
        validateBooking(accom, orderDto.getCheckInDate(), orderDto.getCheckOutDate(),
                adultCount + childCount, null);

        int surcharge = GuestPricingUtils.calculateSurchargePerNight(
                accom.getAccomType(), adultCount, childCount, accom.getPricePerNight());

        OrderItem orderItem = new OrderItem();
        orderItem.setAccom(accom);
        orderItem.setCount(orderDto.getCount());
        orderItem.setOrderPrice(accom.getPricePerNight());
        orderItem.setSurchargePerNight(surcharge);
        orderItem.setAdultCount(adultCount);
        orderItem.setChildCount(childCount);
        orderItem.setGuestCount(adultCount + childCount);
        orderItem.setCheckInDate(orderDto.getCheckInDate());
        orderItem.setCheckOutDate(orderDto.getCheckOutDate());
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

        // Order.cancelOrder() 내부에서 FSM + COMPLETED 방어 처리
        order.cancelOrder();
    }

    // ── 관리자 예약 상태 변경 ──────────────────────────────────────────────
    public void updateOrderStatusByAdmin(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (order.getOrderStatus() == status) {
            return;
        }

        if (status == OrderStatus.CANCEL) {
            // cancelOrder() 가 FSM + COMPLETED 방어를 내부에서 처리
            order.cancelOrder();
            return;
        }

        // CANCEL → ORDER 복구는 FSM 상 불가 (운영 정책상 허용 안 함)
        throw new IllegalArgumentException("관리자도 취소된 주문을 복구할 수 없습니다.");
    }

    // ── 주문 수정 (체크인/체크아웃 변경) ─────────────────────────────────────
    public void updateOrder(Long orderId, OrderUpdateDto dto, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (!order.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        if (!order.getOrderStatus().canCancel()) {
            throw new IllegalStateException("취소된 주문은 수정할 수 없습니다.");
        }

        for (OrderItem item : order.getOrderItems()) {
            // COMPLETED 항목은 수정 불가
            if (!item.getBookingStatus().canCancel()) {
                throw new IllegalStateException("이용 완료된 예약은 수정할 수 없습니다.");
            }

            Accom accom = accomRepository.findWithOperationInfoById(item.getAccom().getId())
                    .orElseThrow(EntityNotFoundException::new);

            validateBooking(accom, dto.getCheckInDate(), dto.getCheckOutDate(),
                    item.getGuestCount(), item.getId());

            item.setCheckInDate(dto.getCheckInDate());
            item.setCheckOutDate(dto.getCheckOutDate());
            item.setCount((int) ChronoUnit.DAYS.between(dto.getCheckInDate(), dto.getCheckOutDate()));
            // 날짜 변경 후 CONFIRMED 유지 (FSM상 이미 CONFIRMED → 재설정)
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

        // CONFIRMED 기준으로만 만실 판단
        return orderStayDateRepository.findSoldOutDates(accomId, roomCount, BookingStatus.CONFIRMED);
    }

    // ── 주문 내역 조회 (페이징) ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OrderHistDto> getOrderList(String email, Pageable pageable) {
        List<Order> orders = orderRepository.findOrders(email, pageable);
        List<OrderHistDto> result = new ArrayList<>();

        for (Order order : orders) {
            OrderHistDto dto = new OrderHistDto(order);
            for (OrderItem item : order.getOrderItems()) {
                String imgUrl = item.getAccom().getAccomImgList().stream()
                        .filter(img -> "Y".equals(img.getRepImgYn()))
                        .map(AccomImg::getImgUrl)
                        .findFirst().orElse("");
                dto.addOrderItemDto(new OrderItemDto(item, imgUrl));
            }
            result.add(dto);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public OrderHistDto getOrderDetail(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);

        if (!order.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("주문 상세 조회 권한이 없습니다.");
        }

        OrderHistDto dto = new OrderHistDto(order);
        for (OrderItem item : order.getOrderItems()) {
            String imgUrl = item.getAccom().getAccomImgList().stream()
                    .filter(img -> "Y".equals(img.getRepImgYn()))
                    .map(AccomImg::getImgUrl)
                    .findFirst().orElse("");
            dto.addOrderItemDto(new OrderItemDto(item, imgUrl));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public Long countOrder(String email) {
        return orderRepository.countOrder(email);
    }

    // ── 만실 여부 (CartService 에서 호출) ────────────────────────────────────
    @Transactional(readOnly = true)
    public boolean isSoldOut(Long accomId, int roomCount, LocalDate date) {
        long confirmed = orderStayDateRepository.countConfirmedByDate(
                accomId, date, BookingStatus.CONFIRMED, null);
        return confirmed >= roomCount;
    }

    @Transactional(readOnly = true)
    public int getRemainingRooms(Long accomId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("泥댄겕??/泥댄겕?꾩썐 ?좎쭨???꾩닔?낅땲??");
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("泥댄겕?꾩썐 ?좎쭨??泥댄겕???좎쭨蹂대떎 ?댄썑?ъ빞 ?⑸땲??");
        }

        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        Integer roomCount = accom.getRoomCount();
        if (roomCount == null || roomCount <= 0) {
            return 0;
        }

        int minRemaining = roomCount;
        LocalDate cursor = checkInDate;
        while (cursor.isBefore(checkOutDate)) {
            long confirmed = orderStayDateRepository.countConfirmedByDate(
                    accomId, cursor, BookingStatus.CONFIRMED, null);
            int remaining = Math.max(0, roomCount - (int) confirmed);
            if (remaining < minRemaining) {
                minRemaining = remaining;
            }
            if (minRemaining == 0) {
                break;
            }
            cursor = cursor.plusDays(1);
        }
        return minRemaining;
    }

    /**
     * 월별 날짜별 잔여 객실 수 조회 (캘린더 badge 표시용)
     *
     * <p>한 달치 CONFIRMED 건수를 GROUP BY 단일 쿼리로 가져온 뒤,
     * 운영일만 남은 객실 수(roomCount - confirmedCount)로 변환해 반환한다.</p>
     *
     * @return key=날짜문자열(yyyy-MM-dd), value=잔여 객실 수
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getMonthlyAvailability(Long accomId, int year, int month) {
        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(EntityNotFoundException::new);

        Integer roomCount = accom.getRoomCount();
        if (roomCount == null || roomCount <= 0) {
            return Collections.emptyMap();
        }

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.plusMonths(1);

        // 날짜별 CONFIRMED 건수 일괄 조회
        List<Object[]> rows = orderStayDateRepository.countConfirmedPerDateRange(
                accomId, from, to, BookingStatus.CONFIRMED);

        Map<LocalDate, Long> confirmedByDate = new HashMap<>();
        for (Object[] row : rows) {
            confirmedByDate.put((LocalDate) row[0], (Long) row[1]);
        }

        // 운영일만 결과에 포함
        Set<LocalDate> operationDaySet = new HashSet<>();
        if (accom.getOperationDayList() != null) {
            for (var opDay : accom.getOperationDayList()) {
                operationDaySet.add(opDay.getOperationDate());
            }
        }

        Map<String, Integer> result = new HashMap<>();
        LocalDate cursor = from;
        while (cursor.isBefore(to)) {
            if (operationDaySet.contains(cursor)) {
                long confirmed = confirmedByDate.getOrDefault(cursor, 0L);
                int remaining  = Math.max(0, roomCount - (int) confirmed);
                result.put(cursor.toString(), remaining);
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    // ── 예약 유효성 검증 (공통) ───────────────────────────────────────────────
    /**
     * 날짜·운영 정책·만실 여부를 검증한다.
     * 만실 카운트는 CONFIRMED 상태만 기준으로 한다.
     */
    public void validateBooking(Accom accom,
                                LocalDate checkInDate,
                                LocalDate checkOutDate,
                                int totalGuestCount,
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
        if (totalGuestCount < 1) {
            throw new IllegalArgumentException("투숙 인원은 1명 이상이어야 합니다.");
        }
        if (totalGuestCount > accom.getGuestCount()) {
            throw new IllegalArgumentException(
                    "투숙 인원이 최대 수용 인원(" + accom.getGuestCount() + "명)을 초과합니다.");
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

        if (policy.getCheckInTime() != null) {
            LocalDate today = LocalDate.now();
            if (checkInDate.isEqual(today) && !LocalTime.now().isBefore(policy.getCheckInTime())) {
                throw new IllegalArgumentException("오늘은 체크인 시간이 지나 예약할 수 없습니다.");
            }
        }

        Set<LocalDate> operationDateSet = accom.getOperationDayList().stream()
                .map(AccomOperationDay::getOperationDate)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        LocalDate today = LocalDate.now();

        for (LocalDate stayDate : stayDates) {
            if (checkInDate.isBefore(today)) {
                throw new IllegalArgumentException("오늘 이전 날짜는 예약할 수 없습니다.");
            }
            if (!operationDateSet.contains(stayDate)) {
                throw new IllegalArgumentException("선택한 날짜 중 운영하지 않는 날짜가 포함되어 있습니다.");
            }

            // ✅ CONFIRMED 기준 만실 카운트
            long confirmedCount = orderStayDateRepository.countConfirmedByDate(
                    accom.getId(), stayDate, BookingStatus.CONFIRMED, excludeOrderItemId);

            if (confirmedCount >= accom.getRoomCount()) {
                throw new IllegalArgumentException(
                        "선택한 날짜 중 예약이 마감된 날짜가 있습니다: " + stayDate);
            }
        }
    }

    // ── 내부 공통 로직 ──────────────────────────────────────────────────────
    private void syncStayDates(OrderItem orderItem, Accom accom,
                               LocalDate checkInDate, LocalDate checkOutDate) {
        orderItem.clearStayDates();
        for (LocalDate d : buildStayDates(checkInDate, checkOutDate)) {
            OrderStayDate osd = new OrderStayDate();
            osd.setAccom(accom);
            osd.setStayDate(d);
            orderItem.addStayDate(osd);
        }
    }

    private List<LocalDate> buildStayDates(LocalDate checkInDate, LocalDate checkOutDate) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate cursor = checkInDate;
        while (cursor.isBefore(checkOutDate)) {
            list.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return list;
    }
}
