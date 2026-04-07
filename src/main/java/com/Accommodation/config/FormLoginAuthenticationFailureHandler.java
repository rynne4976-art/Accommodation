package com.Accommodation.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

public class FormLoginAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // 실패 여부는 쿼리 파라미터로 넘기고 실제 메시지 출력은 컨트롤러/뷰에서 처리합니다.
        String redirectUrl = request.getParameter("redirectUrl");
        if (StringUtils.hasText(redirectUrl)) {
            response.sendRedirect("/members/login?error&redirectUrl="
                    + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
            return;
        }

        response.sendRedirect("/members/login?error");
    }
}
