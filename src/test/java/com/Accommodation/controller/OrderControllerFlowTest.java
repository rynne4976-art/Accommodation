package com.Accommodation.controller;

import com.Accommodation.config.RoleBasedAuthenticationSuccessHandler;
import com.Accommodation.config.SecurityConfig;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.CustomOAuth2UserService;
import com.Accommodation.service.CustomUserDetailsService;
import com.Accommodation.service.GoogleOidcUserService;
import com.Accommodation.service.MemberService;
import com.Accommodation.service.OrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(
        controllers = OrderController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, RoleBasedAuthenticationSuccessHandler.class, CommonViewAttributesAdvice.class})
@ActiveProfiles("test")
class OrderControllerFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private AccomService accomService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private GoogleOidcUserService googleOidcUserService;

    @Test
    @DisplayName("예약 필수 정보가 없으면 예약 폼 대신 마이페이지 정보수정으로 이동한다")
    void orderFormRedirectsToMyPageEditWhenReservationInfoMissing() throws Exception {
        given(memberService.hasRequiredReservationInfo("social@test.com")).willReturn(false);

        mockMvc.perform(get("/orders/accom/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("social@test.com").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/mypage/edit?reservationInfoRequired=true"));
    }

    @Test
    @DisplayName("예약 필수 정보가 없으면 주문 생성 요청도 차단한다")
    void orderCreationReturnsBadRequestWhenReservationInfoMissing() throws Exception {
        given(memberService.hasRequiredReservationInfo("social@test.com")).willReturn(false);

        mockMvc.perform(post("/order")
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
}
