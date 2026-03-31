package com.Accommodation.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * AuditorAwareImpl
 *
 * ▶ BaseEntity의 @CreatedBy, @LastModifiedBy 필드에
 *   "현재 로그인한 사용자"를 자동으로 기록하기 위한 구현체입니다.
 *
 * ▶ Spring Security의 SecurityContext에서 인증 정보를 꺼내
 *   사용자 이름(username)을 반환합니다.
 *
 * ▶ 로그인하지 않은 경우(비인증 상태)에는 "anonymousUser"가 저장됩니다.
 *   → 로그인 기능 구현 후 실제 사용자 이름으로 자동 전환됩니다.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("anonymousUser");
        }

        return Optional.of(authentication.getName());
    }
}
