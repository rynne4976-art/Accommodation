package com.Accommodation.controller;

import com.Accommodation.config.RoleBasedAuthenticationSuccessHandler;
import com.Accommodation.config.SecurityConfig;
import com.Accommodation.dto.OrderSearchDto;
import com.Accommodation.constant.Role;
import com.Accommodation.dto.MemberSearchDto;
import com.Accommodation.exception.AdminException;
import com.Accommodation.exception.ErrorCode;
import com.Accommodation.service.AdminService;
import com.Accommodation.service.CustomOAuth2UserService;
import com.Accommodation.service.CustomUserDetailsService;
import com.Accommodation.service.GoogleOidcUserService;
import com.Accommodation.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, RoleBasedAuthenticationSuccessHandler.class, CommonViewAttributesAdvice.class})
@ActiveProfiles("test")
class AdminControllerFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private GoogleOidcUserService googleOidcUserService;

    @Test
    @DisplayName("없는 회원 상세 조회는 관리자 회원 목록으로 리다이렉트한다")
    void missingMemberDetailRedirectsToMemberList() throws Exception {
        given(adminService.getMemberDetail(99999L))
                .willThrow(new AdminException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));
        given(adminService.getMemberPage(any(MemberSearchDto.class), anyString(), any()))
                .willReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/admin/members/99999")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/members**"));
    }

    @Test
    @DisplayName("회원 관리 검색 조건은 화면에 유지된다")
    void memberManageKeepsSearchFilters() throws Exception {
        given(adminService.getMemberPage(any(MemberSearchDto.class), anyString(), any()))
                .willReturn(new PageImpl<>(Collections.emptyList()));

                mockMvc.perform(get("/admin/members")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .param("searchBy", "email")
                        .param("searchQuery", "kim@test.com")
                .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"kim@test.com\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"email\" selected=\"selected\">이메일</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"ADMIN\" selected=\"selected\">ADMIN</option>")));
    }

    @Test
    @DisplayName("관리자가 자신의 ADMIN 권한을 해제하려고 하면 목록으로 리다이렉트한다")
    void selfRoleDowngradeRedirectsToMemberList() throws Exception {
        willThrow(new AdminException(ErrorCode.ADMIN_ROLE_DOWNGRADE_FORBIDDEN))
                .given(adminService)
                .updateMemberRole(anyLong(), any(Role.class), anyString());

        mockMvc.perform(post("/admin/members/1/role")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/members**"));
    }

    @Test
    @DisplayName("권한 변경 후에도 회원 관리 검색 조건과 페이지가 유지된다")
    void roleUpdateRedirectKeepsMemberSearchFilters() throws Exception {
        mockMvc.perform(post("/admin/members/1/role")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("role", "USER")
                        .param("page", "2")
                        .param("searchBy", "name")
                        .param("searchQuery", "kim")
                        .param("filterRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/members?roleUpdated=true&page=2&searchBy=name&searchQuery=kim&role=ADMIN*"));
    }

    @Test
    @DisplayName("예약 관리 검색 조건은 화면에 유지된다")
    void orderManageKeepsSearchFilters() throws Exception {
        given(adminService.getOrderPage(any(OrderSearchDto.class), any()))
                .willReturn(new PageImpl<>(Collections.emptyList()));

                mockMvc.perform(get("/admin/orders")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .param("searchBy", "email")
                        .param("searchQuery", "kim@test.com")
                .param("orderStatus", "CANCEL"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"kim@test.com\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"email\" selected=\"selected\">이메일</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"CANCEL\" selected=\"selected\">예약 취소</option>")));
    }
}
