package com.Accommodation.service;

import com.Accommodation.dto.CartItemDto;
import com.Accommodation.dto.CartListItemDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.entity.CartItem;
import com.Accommodation.entity.Member;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.CartItemRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.OrderStayDateRepository;
import com.Accommodation.constant.OrderStatus;
import com.Accommodation.util.GuestPricingUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 장바구니(예약 대기) 서비스
 *
 * <p>흐름: 예약하기 폼 → 장바구니 추가(PENDING) → 예약 확정(CONFIRMED → Order 생성)</p>
 * <ul>
 *   <li>장바구니 추가 시: roomCount 변경 없음, CONFIRMED 예약 기준으로 만실 여부만 체크</li>
 *   <li>예약 확정 시: 재검증 후 Order 생성, 만실이 된 날짜는 다른 사용자 장바구니에 SSE 알림</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final AccomRepository accomRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final OrderStayDateRepository orderStayDateRepository;

    // ── 장바구니 추가 ────────────────────────────────────────────────────────
    /**
     * 예약 대기(장바구니)에 항목을 추가한다.
     * roomCount 는 줄이지 않으며, CONFIRMED 예약 기준으로 만실 여부만 확인한다.
     *
     * @return 생성된 CartItem ID
     */
    public Long addCartItem(CartItemDto dto, String email) {

        Accom accom = accomRepository.findWithOperationInfoById(dto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new EntityNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        // 1) 유형별 인원 제한 검증
        GuestPricingUtils.validateGuestCount(accom.getAccomType(), dto.getAdultCount(), dto.getChildCount());

        // 2) 날짜·운영일·만실 검증 (CONFIRMED 예약 기준)
        orderService.validateBooking(
                accom,
                dto.getCheckInDate(),
                dto.getCheckOutDate(),
                dto.getAdultCount() + dto.getChildCount(),
                null
        );

        CartItem cartItem = new CartItem();
        cartItem.setMember(member);
        cartItem.setAccom(accom);
        cartItem.setCheckInDate(dto.getCheckInDate());
        cartItem.setCheckOutDate(dto.getCheckOutDate());
        cartItem.setAdultCount(dto.getAdultCount());
        cartItem.setChildCount(dto.getChildCount());

        cartItemRepository.save(cartItem);
        return cartItem.getId();
    }

    // ── 장바구니 목록 조회 ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CartListItemDto> getCartItems(String email) {
        return cartItemRepository.findByMemberEmailOrderByRegTimeDesc(email).stream()
                .map(cartItem -> {
                    String repImgUrl = cartItem.getAccom().getAccomImgList().stream()
                            .filter(img -> "Y".equals(img.getRepImgYn()))
                            .map(AccomImg::getImgUrl)
                            .findFirst()
                            .orElse("");
                    return new CartListItemDto(cartItem, repImgUrl);
                })
                .toList();
    }

    // ── 장바구니 항목 삭제 ────────────────────────────────────────────────────
    public void removeCartItem(Long cartItemId, String email) {
        CartItem cartItem = cartItemRepository.findByIdAndMemberEmail(cartItemId, email)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목을 찾을 수 없습니다."));
        cartItemRepository.delete(cartItem);
    }

    // ── 예약 확정 (개별) ─────────────────────────────────────────────────────
    /**
     * 장바구니의 단일 항목을 예약 확정으로 전환한다.
     *
     * <ol>
     *   <li>재검증 (확정 시점에 만실이면 예외)</li>
     *   <li>Order 생성 (roomCount 기반 만실 카운트에 추가됨)</li>
     *   <li>CartItem 삭제</li>
     *   <li>만실이 된 날짜에 장바구니를 가진 다른 사용자에게 SSE 알림</li>
     * </ol>
     *
     * @return 생성된 Order ID
     */
    public Long confirmCartItem(Long cartItemId, String email) {

        CartItem cartItem = cartItemRepository.findByIdAndMemberEmail(cartItemId, email)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목을 찾을 수 없습니다."));

        Accom accom = accomRepository.findWithOperationInfoById(cartItem.getAccom().getId())
                .orElseThrow(EntityNotFoundException::new);

        // 확정 시 재검증
        orderService.validateBooking(
                accom,
                cartItem.getCheckInDate(),
                cartItem.getCheckOutDate(),
                cartItem.getAdultCount() + cartItem.getChildCount(),
                null
        );

        // Order 생성
        Long orderId = orderService.createOrderFromCartItem(cartItem, email);

        // 장바구니에서 제거
        cartItemRepository.delete(cartItem);

        // 만실 알림
        notifySoldOutIfNeeded(accom, cartItem.getCheckInDate(), cartItem.getCheckOutDate(), email);

        return orderId;
    }

    // ── 예약 확정 (전체) ─────────────────────────────────────────────────────
    /**
     * 장바구니의 모든 항목을 순서대로 예약 확정한다.
     * 하나라도 실패하면 트랜잭션이 롤백된다.
     *
     * @return 생성된 Order ID 목록
     */
    public List<Long> confirmAllCartItems(String email) {
        List<CartItem> cartItems = cartItemRepository.findByMemberEmailOrderByRegTimeDesc(email);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있습니다.");
        }

        List<Long> orderIds = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Accom accom = accomRepository.findWithOperationInfoById(cartItem.getAccom().getId())
                    .orElseThrow(EntityNotFoundException::new);

            orderService.validateBooking(
                    accom,
                    cartItem.getCheckInDate(),
                    cartItem.getCheckOutDate(),
                    cartItem.getAdultCount() + cartItem.getChildCount(),
                    null
            );

            Long orderId = orderService.createOrderFromCartItem(cartItem, email);
            orderIds.add(orderId);

            cartItemRepository.delete(cartItem);
            notifySoldOutIfNeeded(accom, cartItem.getCheckInDate(), cartItem.getCheckOutDate(), email);
        }
        return orderIds;
    }

    // ── 만실 알림 ─────────────────────────────────────────────────────────────
    /**
     * 예약 확정 후 각 날짜가 만실이 됐는지 확인하고,
     * 해당 날짜를 장바구니에 담은 다른 사용자에게 SSE 알림을 보낸다.
     */
    private void notifySoldOutIfNeeded(Accom accom,
                                       LocalDate checkInDate,
                                       LocalDate checkOutDate,
                                       String confirmerEmail) {

        Integer roomCount = accom.getRoomCount();
        if (roomCount == null || roomCount <= 0) return;

        LocalDate cursor = checkInDate;
        while (cursor.isBefore(checkOutDate)) {
            if (orderService.isSoldOut(accom.getId(), roomCount, cursor)) {
                // 이 날짜가 들어간 다른 사용자의 CartItem 조회
                List<CartItem> affected = cartItemRepository.findOtherUsersCartItemsByAccomAndDate(
                        accom.getId(), cursor, confirmerEmail);

                for (CartItem other : affected) {
                    notificationService.sendSoldOutAlert(
                            other.getMember().getEmail(),
                            accom.getAccomName(),
                            cursor
                    );
                }
            }
            cursor = cursor.plusDays(1);
        }
    }
}
