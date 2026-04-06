package com.Accommodation.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");

        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 미인증 접근은 로그인 화면으로 이동시킵니다.
        response.sendRedirect("/members/login");
    }
}
