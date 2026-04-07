package com.Accommodation.config;

import com.Accommodation.controller.MainController;
import com.Accommodation.controller.MemberController;
import com.Accommodation.controller.CommonViewAttributesAdvice;
import com.Accommodation.constant.Role;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.entity.Member;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {MainController.class, MemberController.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, RoleBasedAuthenticationSuccessHandler.class, CommonViewAttributesAdvice.class})
@ActiveProfiles("test")
class SecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @MockBean
    private MemberService memberService;

    @MockBean
    private AccomService accomService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private GoogleOidcUserService googleOidcUserService;

    private String encodedPassword(String rawPassword) {
        return "{bcrypt}" + passwordEncoder.encode(rawPassword);
    }

    @Test
    @DisplayName("CSRF 토큰이 포함된 로그인은 메인으로 이동한다")
    void loginSuccess() throws Exception {
        UserDetails userDetails = User.withUsername("dummy@example.com")
                .password(encodedPassword("password123"))
                .roles("USER")
                .build();

        given(customUserDetailsService.loadUserByUsername("dummy@example.com"))
                .willReturn(userDetails);

        mockMvc.perform(formLogin("/members/login")
                        .user("email", "dummy@example.com")
                        .password("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"))
                .andExpect(authenticated().withUsername("dummy@example.com"));
    }

    @Test
    @DisplayName("관리자 로그인은 메인으로 이동한다")
    void adminLoginRedirectsToDashboard() throws Exception {
        UserDetails userDetails = User.withUsername("admin@accom.com")
                .password(encodedPassword("password123"))
                .roles("ADMIN")
                .build();

        given(customUserDetailsService.loadUserByUsername("admin@accom.com"))
                .willReturn(userDetails);

        mockMvc.perform(formLogin("/members/login")
                        .user("email", "admin@accom.com")
                        .password("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"))
                .andExpect(authenticated().withUsername("admin@accom.com"));
    }

    @Test
    @DisplayName("보호된 페이지 접근 후 로그인하면 원래 보던 화면으로 돌아간다")
    void loginRedirectsToSavedRequest() throws Exception {
        UserDetails userDetails = User.withUsername("dummy@example.com")
                .password(encodedPassword("password123"))
                .roles("USER")
                .build();

        given(customUserDetailsService.loadUserByUsername("dummy@example.com"))
                .willReturn(userDetails);

        var savedSession = mockMvc.perform(get("/members/mypage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(post("/members/login")
                        .with(csrf())
                .session((org.springframework.mock.web.MockHttpSession) savedSession)
                        .param("email", "dummy@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/members/mypage**"))
                .andExpect(authenticated().withUsername("dummy@example.com"));
    }

    @Test
    @DisplayName("CSRF 토큰이 없는 로그인은 403이다")
    void loginWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/members/login")
                        .param("email", "dummy@example.com")
                        .param("password", "password123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인한 사용자의 로그아웃은 메인으로 이동한다")
    void logoutSuccess() throws Exception {
        mockMvc.perform(post("/members/logout")
                        .with(user("dummy@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main?logout"));
    }

    @Test
    @DisplayName("CSRF 토큰이 없는 로그아웃은 403이다")
    void logoutWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/members/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인 실패 시 에러 파라미터가 포함된 로그인 페이지를 반환한다")
    void loginFailureRedirectsToLoginPage() throws Exception {
        given(customUserDetailsService.loadUserByUsername(anyString()))
                .willThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("not found"));

        mockMvc.perform(formLogin("/members/login")
                        .user("email", "missing@example.com")
                        .password("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login?error"));
    }

    @Test
    @DisplayName("로그인 페이지는 에러 메시지를 노출할 수 있다")
    void loginPageWithErrorLoads() throws Exception {
        mockMvc.perform(get("/members/login").param("error", ""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인한 사용자는 마이페이지를 볼 수 있다")
    void myPageLoads() throws Exception {
        Member member = new Member();
        member.setName("김테스트");
        member.setEmail("kim@test.com");
        member.setPassword(encodedPassword("password123"));
        member.setNumber("01012345678");
        member.setAddress("서울시 강남구");
        member.setRole(Role.USER);

        given(memberService.getMemberByEmail("kim@test.com")).willReturn(member);

        mockMvc.perform(get("/members/mypage")
                        .with(user("kim@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("김테스트님, 반갑습니다.")));
    }

    @Test
    @DisplayName("호텔 목록 페이지는 정상 렌더링된다")
    void hotelsPageLoads() throws Exception {
        given(accomService.getMainAccomPage(any(), any(Pageable.class)))
                .willReturn(new PageImpl<MainAccomDto>(Collections.emptyList()));

        mockMvc.perform(get("/main/hotels"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("검색 결과 페이지는 정상 렌더링된다")
    void searchListPageLoads() throws Exception {
        given(accomService.getMainAccomPage(any(), any(Pageable.class)))
                .willReturn(new PageImpl<MainAccomDto>(Collections.emptyList()));

        mockMvc.perform(get("/searchList").param("searchQuery", "서울"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 페이지에서 로그인하면 redirectUrl로 원래 화면에 돌아간다")
    void loginRedirectsToExplicitRedirectUrl() throws Exception {
        UserDetails userDetails = User.withUsername("dummy@example.com")
                .password(encodedPassword("password123"))
                .roles("USER")
                .build();

        given(customUserDetailsService.loadUserByUsername("dummy@example.com"))
                .willReturn(userDetails);

        mockMvc.perform(post("/members/login")
                        .with(csrf())
                        .param("email", "dummy@example.com")
                        .param("password", "password123")
                        .param("redirectUrl", "/accom/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accom/7"))
                .andExpect(authenticated().withUsername("dummy@example.com"));
    }

    @Test
    @DisplayName("로그인 성공 시 redirectUrl의 logout 파라미터는 제거된다")
    void loginRedirectStripsLogoutQuery() throws Exception {
        UserDetails userDetails = User.withUsername("dummy@example.com")
                .password(encodedPassword("password123"))
                .roles("USER")
                .build();

        given(customUserDetailsService.loadUserByUsername("dummy@example.com"))
                .willReturn(userDetails);

        mockMvc.perform(post("/members/login")
                        .with(csrf())
                        .param("email", "dummy@example.com")
                        .param("password", "password123")
                        .param("redirectUrl", "/main?logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"))
                .andExpect(authenticated().withUsername("dummy@example.com"));
    }
}
