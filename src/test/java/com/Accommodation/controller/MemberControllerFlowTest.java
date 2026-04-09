package com.Accommodation.controller;

import com.Accommodation.config.RoleBasedAuthenticationSuccessHandler;
import com.Accommodation.config.SecurityConfig;
import com.Accommodation.constant.Role;
import com.Accommodation.entity.Member;
import com.Accommodation.exception.ErrorCode;
import com.Accommodation.exception.MemberException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MemberController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, RoleBasedAuthenticationSuccessHandler.class, CommonViewAttributesAdvice.class})
@ActiveProfiles("test")
class MemberControllerFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private GoogleOidcUserService googleOidcUserService;

    @Test
    @DisplayName("중복 이메일 회원가입은 회원가입 페이지에 에러 메시지를 남긴다")
    void signupDuplicateEmailShowsErrorMessage() throws Exception {
        given(memberService.saveMember(any()))
                .willThrow(new MemberException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/members/new")
                        .with(csrf())
                        .param("name", "테스트회원")
                        .param("email", "dup@test.com")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!")
                        .param("number", "01012345678")
                        .param("postcode", "12345")
                        .param("address", "서울시 강남구")
                        .param("detailAddress", "101호"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("이미 가입된 회원입니다.")));
    }

    @Test
    @DisplayName("이메일 중복 확인 API는 중복 여부를 반환한다")
    void checkEmailDuplicateReturnsDuplicateFlag() throws Exception {
        given(memberService.existsByEmail("dup@test.com")).willReturn(true);

        mockMvc.perform(get("/members/check-email").param("email", "dup@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"duplicate\":true}"));
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호가 틀리면 같은 화면에 에러 메시지를 남긴다")
    void passwordChangeWithWrongCurrentPasswordShowsErrorMessage() throws Exception {
        Member member = new Member();
        member.setEmail("kim@test.com");
        member.setName("김테스트");
        member.setPassword("encoded");
        member.setRole(Role.USER);

        given(memberService.getMemberByEmail("kim@test.com")).willReturn(member);
        willThrow(new MemberException(ErrorCode.INVALID_CURRENT_PASSWORD))
                .given(memberService)
                .changePassword(anyString(), any());

        mockMvc.perform(post("/members/mypage/password")
                        .with(SecurityMockMvcRequestPostProcessors.user("kim@test.com").roles("USER"))
                        .with(csrf())
                        .param("currentPassword", "Wrong123!")
                        .param("newPassword", "NewPassword123!")
                        .param("confirmPassword", "NewPassword123!"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("현재 비밀번호가 일치하지 않습니다.")));
    }
}
