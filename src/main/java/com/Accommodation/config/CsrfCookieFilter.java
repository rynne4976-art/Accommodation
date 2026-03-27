package com.Accommodation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");

        if (csrfToken != null) {
            Cookie cookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        filterChain.doFilter(request, response);
    }
}