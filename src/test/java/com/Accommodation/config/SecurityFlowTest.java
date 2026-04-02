package com.Accommodation.config;

import com.Accommodation.controller.MainController;
import com.Accommodation.controller.MemberController;
import com.Accommodation.constant.Role;
import com.Accommodation.entity.Member;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.CustomUserDetailsService;
import com.Accommodation.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {MainController.class, MemberController.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, RoleBasedAuthenticationSuccessHandler.class})
class SecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private MemberService memberService;

    @MockBean
    private AccomService accomService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("CSRF 토큰이 포함된 로그인은 메인으로 이동한다")
    void loginSuccess() throws Exception {
        UserDetails userDetails = User.withUsername("dummy@example.com")
                .password(passwordEncoder.encode("password123"))
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
    @DisplayName("관리자 로그인은 관리자 대시보드로 이동한다")
    void adminLoginRedirectsToDashboard() throws Exception {
        UserDetails userDetails = User.withUsername("admin@accom.com")
                .password(passwordEncoder.encode("password123"))
                .roles("ADMIN")
                .build();

        given(customUserDetailsService.loadUserByUsername("admin@accom.com"))
                .willReturn(userDetails);

        mockMvc.perform(formLogin("/members/login")
                        .user("email", "admin@accom.com")
                        .password("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername("admin@accom.com"));
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
        member.setPassword(passwordEncoder.encode("password123"));
        member.setNumber("01012345678");
        member.setAddress("서울시 강남구");
        member.setRole(Role.USER);

        given(memberService.getMemberByEmail("kim@test.com")).willReturn(member);

        mockMvc.perform(get("/members/mypage")
                        .with(user("kim@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("김테스트님, 반갑습니다.")));
    }
}
