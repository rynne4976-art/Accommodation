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
 * ?λ컮援щ땲(?덉빟 ?湲? ?쒕퉬??
 *
 * <p>?먮쫫: ?덉빟?섍린 ?????λ컮援щ땲 異붽?(PENDING) ???덉빟 ?뺤젙(CONFIRMED ??Order ?앹꽦)</p>
 * <ul>
 *   <li>?λ컮援щ땲 異붽? ?? roomCount 蹂寃??놁쓬, CONFIRMED ?덉빟 湲곗??쇰줈 留뚯떎 ?щ?留?泥댄겕</li>
 *   <li>?덉빟 ?뺤젙 ?? ?ш?利???Order ?앹꽦, 留뚯떎?????좎쭨???ㅻⅨ ?ъ슜???λ컮援щ땲??SSE ?뚮┝</li>
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
     * ?덉빟 ?湲??λ컮援щ땲)????ぉ??異붽??쒕떎.
     * roomCount ??以꾩씠吏 ?딆쑝硫? CONFIRMED ?덉빟 湲곗??쇰줈 留뚯떎 ?щ?留??뺤씤?쒕떎.
     */
    public Long addCartItem(CartItemDto dto, String email) {
        Accom accom = accomRepository.findWithOperationInfoById(dto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new EntityNotFoundException("?뚯썝 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.");
        }

        GuestPricingUtils.validateGuestCount(accom.getAccomType(), dto.getAdultCount(), dto.getChildCount());

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
                                null
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
                .orElseThrow(() -> new EntityNotFoundException("?λ컮援щ땲 ??ぉ??李얠쓣 ???놁뒿?덈떎."));
        cartItemRepository.delete(cartItem);
    }

    public void updateCartItem(Long cartItemId, CartItemDto dto, String email) {
        CartItem cartItem = cartItemRepository.findByIdAndMemberEmail(cartItemId, email)
                .orElseThrow(() -> new EntityNotFoundException("?λ컮援щ땲 ??ぉ??李얠쓣 ???놁뒿?덈떎."));

        Accom accom = accomRepository.findWithOperationInfoById(dto.getAccomId())
                .orElseThrow(EntityNotFoundException::new);

        GuestPricingUtils.validateGuestCount(accom.getAccomType(), dto.getAdultCount(), dto.getChildCount());

        orderService.validateBooking(
                accom,
                dto.getCheckInDate(),
                dto.getCheckOutDate(),
                dto.getAdultCount() + dto.getChildCount(),
                null
        );

        cartItem.setAccom(accom);
        cartItem.setCheckInDate(dto.getCheckInDate());
        cartItem.setCheckOutDate(dto.getCheckOutDate());
        cartItem.setAdultCount(dto.getAdultCount());
        cartItem.setChildCount(dto.getChildCount());
    }

    /**
     * ?λ컮援щ땲???⑥씪 ??ぉ???덉빟 ?뺤젙?쇰줈 ?꾪솚?쒕떎.
     */
    public Long confirmCartItem(Long cartItemId, String email) {
        CartItem cartItem = cartItemRepository.findByIdAndMemberEmail(cartItemId, email)
                .orElseThrow(() -> new EntityNotFoundException("?λ컮援щ땲 ??ぉ??李얠쓣 ???놁뒿?덈떎."));

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
        cartItemRepository.delete(cartItem);
        return orderId;
    }

    /**
     * ?λ컮援щ땲??紐⑤뱺 ??ぉ???쒖꽌?濡??덉빟 ?뺤젙?쒕떎.
     */
    public List<Long> confirmAllCartItems(String email) {
        List<CartItem> cartItems = cartItemRepository.findByMemberEmailOrderByRegTimeDesc(email);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("?λ컮援щ땲媛 鍮꾩뼱 ?덉뒿?덈떎.");
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
        }
        return orderIds;
    }
}
