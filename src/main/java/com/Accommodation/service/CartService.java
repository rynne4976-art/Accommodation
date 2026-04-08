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
import com.Accommodation.util.GuestPricingUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 장바구니(예약 대기) 서비스.
 *
 * <p>흐름: 예약하기 버튼은 장바구니 추가(PENDING) 또는 예약 확정(CONFIRMED 주문 생성)으로 이어진다.</p>
 * <ul>
 *   <li>장바구니 추가 시 객실 수(roomCount)는 차감하지 않고, CONFIRMED 예약 기준으로만 재고를 확인한다.</li>
 *   <li>예약 확정 시 재검증 후 Order를 생성하고, 만실된 날짜가 있으면 다른 사용자의 장바구니도 영향을 받는다.</li>
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

    /**
     * 예약 대기(장바구니) 항목을 추가한다.
     * roomCount는 즉시 차감하지 않으며, CONFIRMED 예약 기준으로만 재고를 확인한다.
     */
    public Long addCartItem(CartItemDto dto, String email) {
        Accom accom = accomRepository.findWithOperationInfoById(dto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new EntityNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        GuestPricingUtils.validateGuestCount(accom.getAccomType(), dto.getAdultCount(), dto.getChildCount());

        orderService.validateBooking(
                accom,
                dto.getCheckInDate(),
                dto.getCheckOutDate(),
                dto.getAdultCount() + dto.getChildCount(),
                null,
                dto.getRoomCount()
        );

        CartItem cartItem = new CartItem();
        cartItem.setMember(member);
        cartItem.setAccom(accom);
        cartItem.setCheckInDate(dto.getCheckInDate());
        cartItem.setCheckOutDate(dto.getCheckOutDate());
        cartItem.setAdultCount(dto.getAdultCount());
        cartItem.setChildCount(dto.getChildCount());
        cartItem.setRoomCount(dto.getRoomCount());

        cartItemRepository.save(cartItem);
        return cartItem.getId();
    }

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

    @Transactional(readOnly = true)
    public CartListItemDto findFirstUnavailableCartItem(String email) {
        return cartItemRepository.findByMemberEmailOrderByRegTimeDesc(email).stream()
                .map(cartItem -> {
                    try {
                        orderService.validateBooking(
                                cartItem.getAccom(),
                                cartItem.getCheckInDate(),
                                cartItem.getCheckOutDate(),
                                cartItem.getAdultCount() + cartItem.getChildCount(),
                                null,
                                cartItem.getRoomCount()
                        );
                        return null;
                    } catch (Exception ex) {
                        String repImgUrl = cartItem.getAccom().getAccomImgList().stream()
                                .filter(img -> "Y".equals(img.getRepImgYn()))
                                .map(AccomImg::getImgUrl)
                                .findFirst()
                                .orElse("");
                        return new CartListItemDto(cartItem, repImgUrl);
                    }
                })
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
    }

    public void removeCartItem(Long cartItemId, String email) {
        CartItem cartItem = cartItemRepository.findByIdAndMemberEmail(cartItemId, email)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목을 찾을 수 없습니다."));
        cartItemRepository.delete(cartItem);
    }

    public void updateCartItem(Long cartItemId, CartItemDto dto, String email) {
        CartItem cartItem = cartItemRepository.findByIdAndMemberEmail(cartItemId, email)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목을 찾을 수 없습니다."));

        Accom accom = accomRepository.findWithOperationInfoById(dto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        GuestPricingUtils.validateGuestCount(accom.getAccomType(), dto.getAdultCount(), dto.getChildCount());

        orderService.validateBooking(
                accom,
                dto.getCheckInDate(),
                dto.getCheckOutDate(),
                dto.getAdultCount() + dto.getChildCount(),
                null,
                dto.getRoomCount()
        );

        cartItem.setAccom(accom);
        cartItem.setCheckInDate(dto.getCheckInDate());
        cartItem.setCheckOutDate(dto.getCheckOutDate());
        cartItem.setAdultCount(dto.getAdultCount());
        cartItem.setChildCount(dto.getChildCount());
        cartItem.setRoomCount(dto.getRoomCount());
    }

    /**
     * 장바구니의 단일 항목을 예약 확정으로 전환한다.
     */
    public Long confirmCartItem(Long cartItemId, String email) {
        CartItem cartItem = cartItemRepository.findByIdAndMemberEmail(cartItemId, email)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목을 찾을 수 없습니다."));

        Accom accom = accomRepository.findWithOperationInfoById(cartItem.getAccom().getId())
                .orElseThrow(EntityNotFoundException::new);

        orderService.validateBooking(
                accom,
                cartItem.getCheckInDate(),
                cartItem.getCheckOutDate(),
                cartItem.getAdultCount() + cartItem.getChildCount(),
                null,
                cartItem.getRoomCount()
        );

        Long orderId = orderService.createOrderFromCartItem(cartItem, email);
        cartItemRepository.delete(cartItem);
        return orderId;
    }

    /**
     * 장바구니의 모든 항목을 순서대로 예약 확정한다.
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
                    null,
                    cartItem.getRoomCount()
            );

            Long orderId = orderService.createOrderFromCartItem(cartItem, email);
            orderIds.add(orderId);
            cartItemRepository.delete(cartItem);
        }
        return orderIds;
    }
}
