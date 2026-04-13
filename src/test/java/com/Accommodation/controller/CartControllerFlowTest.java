package com.Accommodation.controller;

import com.Accommodation.config.RoleBasedAuthenticationSuccessHandler;
import com.Accommodation.config.SecurityConfig;
import com.Accommodation.service.CartService;
import com.Accommodation.service.CustomOAuth2UserService;
import com.Accommodation.service.CustomUserDetailsService;
import com.Accommodation.service.GoogleOidcUserService;
import com.Accommodation.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CartController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, RoleBasedAuthenticationSuccessHandler.class, CommonViewAttributesAdvice.class})
@ActiveProfiles("test")
class CartControllerFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private GoogleOidcUserService googleOidcUserService;

    @Test
    @DisplayName("예약 필수 정보가 없으면 장바구니 담기를 차단한다")
    void addToCartReturnsBadRequestWhenReservationInfoMissing() throws Exception {
        given(memberService.hasRequiredReservationInfo("social@test.com")).willReturn(false);

        mockMvc.perform(post("/cart")
                        .with(SecurityMockMvcRequestPostProcessors.user("social@test.com").roles("USER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "accomId": 1,
                                  "checkInDate": "2026-04-20",
                                  "checkOutDate": "2026-04-21",
                                  "adultCount": 1,
                                  "childCount": 0,
                                  "roomCount": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("예약을 위해 연락처와 주소를 먼저 입력해주세요."));
    }

    @Test
    @DisplayName("예약 필수 정보가 없으면 장바구니 단건 예약확정을 차단한다")
    void confirmCartItemReturnsBadRequestWhenReservationInfoMissing() throws Exception {
        given(memberService.hasRequiredReservationInfo("social@test.com")).willReturn(false);

        mockMvc.perform(post("/cart/1/confirm")
                        .with(SecurityMockMvcRequestPostProcessors.user("social@test.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("예약을 위해 연락처와 주소를 먼저 입력해주세요."));
    }

    @Test
    @DisplayName("예약 필수 정보가 없으면 장바구니 선택 예약확정을 차단한다")
    void confirmSelectedCartItemsReturnsBadRequestWhenReservationInfoMissing() throws Exception {
        given(memberService.hasRequiredReservationInfo("social@test.com")).willReturn(false);

        mockMvc.perform(post("/cart/confirm-selected")
                        .with(SecurityMockMvcRequestPostProcessors.user("social@test.com").roles("USER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "cartItemIds": [1, 2]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("예약을 위해 연락처와 주소를 먼저 입력해주세요."));
    }
}
