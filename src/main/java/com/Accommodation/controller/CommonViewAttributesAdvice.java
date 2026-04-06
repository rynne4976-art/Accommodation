package com.Accommodation.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class CommonViewAttributesAdvice {

    private final Environment environment;

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

    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() {
        return StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id"))
                && StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-secret"));
    }

    @ModelAttribute("kakaoLoginEnabled")
    public boolean kakaoLoginEnabled() {
        return StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.kakao.client-id"));
    }
}
