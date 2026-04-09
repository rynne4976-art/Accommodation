package com.Accommodation.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String redirectUrl = sanitizeRedirectUrl(request.getParameter("redirectUrl"));
        if (!StringUtils.hasText(redirectUrl) && request.getSession(false) != null) {
            Object sessionRedirectUrl = request.getSession(false).getAttribute("redirectUrl");
            if (sessionRedirectUrl instanceof String sessionValue) {
                redirectUrl = sanitizeRedirectUrl(sessionValue);
            }
        }
        if (isSafeRedirectUrl(redirectUrl)) {
            clearRedirectUrl(request);
            response.sendRedirect(redirectUrl);
            return;
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            clearRedirectUrl(request);
            new SavedRequestAwareAuthenticationSuccessHandler()
                    .onAuthenticationSuccess(request, response, authentication);
            return;
        }

        clearRedirectUrl(request);
        response.sendRedirect("/main");
    }

    private void clearRedirectUrl(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute("redirectUrl");
        }
    }

    private boolean isSafeRedirectUrl(String redirectUrl) {
        return StringUtils.hasText(redirectUrl)
                && redirectUrl.startsWith("/")
                && !redirectUrl.startsWith("//")
                && !redirectUrl.startsWith("/members/login")
                && !redirectUrl.startsWith("/members/logout");
    }

    private String sanitizeRedirectUrl(String redirectUrl) {
        if (!StringUtils.hasText(redirectUrl)) {
            return redirectUrl;
        }

        int queryIndex = redirectUrl.indexOf('?');
        if (queryIndex < 0) {
            return redirectUrl;
        }

        String path = redirectUrl.substring(0, queryIndex);
        String query = redirectUrl.substring(queryIndex + 1);
        String filteredQuery = Arrays.stream(query.split("&"))
                .filter(StringUtils::hasText)
                .filter(param -> !param.startsWith("logout="))
                .filter(param -> !"logout".equals(param))
                .collect(Collectors.joining("&"));

        if (!StringUtils.hasText(filteredQuery)) {
            return path;
        }

        return path + "?" + filteredQuery;
    }
}
