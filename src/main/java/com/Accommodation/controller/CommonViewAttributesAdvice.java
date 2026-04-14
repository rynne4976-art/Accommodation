package com.Accommodation.controller;

import com.Accommodation.config.AuthenticatedMember;
import com.Accommodation.entity.Member;
import com.Accommodation.exception.MemberException;
import com.Accommodation.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class CommonViewAttributesAdvice {

    private final Environment environment;
    private final ObjectProvider<MemberService> memberServiceProvider;

    @ModelAttribute("adminView")
    public boolean adminView(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.startsWith("/admin");
    }

    @ModelAttribute("adminDashboard")
    public boolean adminDashboard(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "/admin".equals(requestUri);
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentPathWithQuery")
    public String currentPathWithQuery(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();

        if (!StringUtils.hasText(queryString)) {
            return requestUri;
        }

        String filteredQuery = Arrays.stream(queryString.split("&"))
                .filter(StringUtils::hasText)
                .filter(param -> !param.startsWith("logout="))
                .filter(param -> !"logout".equals(param))
                .collect(Collectors.joining("&"));

        if (!StringUtils.hasText(filteredQuery)) {
            return requestUri;
        }

        return requestUri + "?" + filteredQuery;
    }

    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() {
        return StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id"))
                && StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-secret"));
    }

    @ModelAttribute("kakaoLoginEnabled")
    public boolean kakaoLoginEnabled() {
        return StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.kakao.client-id"));
    }

    @ModelAttribute("currentMemberDisplayName")
    public String currentMemberDisplayName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String email = authentication.getName();
        String fallbackName = authentication.getName();

        if (principal instanceof AuthenticatedMember authenticatedMember) {
            email = authenticatedMember.getUsername();
            fallbackName = authenticatedMember.getName();
        }

        if (!StringUtils.hasText(email) || "anonymousUser".equals(email)) {
            return null;
        }

        try {
            MemberService memberService = memberServiceProvider.getIfAvailable();
            if (memberService == null) {
                return fallbackName;
            }

            Member member = memberService.getMemberByEmail(email);
            if (member == null) {
                return fallbackName;
            }
            return StringUtils.hasText(member.getName()) ? member.getName() : fallbackName;
        } catch (MemberException e) {
            return fallbackName;
        }
    }
}
