package com.Accommodation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.util.StringUtils;

import com.Accommodation.service.CustomOAuth2UserService;
import com.Accommodation.service.GoogleOidcUserService;

/**
 * 🧾 SecurityConfig (보안 설정)
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final RoleBasedAuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final GoogleOidcUserService googleOidcUserService;
    private final Environment environment;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    /**
     * 🌐 보안 필터 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        HttpSecurity security = http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/main",
                                "/main/**",
                                "/searchList",
                                "/recent-viewed",
                                "/reviews/accom/**",
                                "/orders/accom/*/availability",
                                "/orders/accom/*/monthly-availability",
                                "/error",
                                "/error/**",
                                "/images/**",
                                "/accom/**"
                        ).permitAll()
                        .requestMatchers(
                                "/members/new",
                                "/members/login",
                                "/css/**",
                                "/js/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // 미인증 접근은 로그인 페이지로 유도해 일반 웹 흐름에 맞춥니다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedPage("/error/403")
                )

                .formLogin(form -> form
                        .loginPage("/members/login")
                        .loginProcessingUrl("/members/login")
                        // 로그인 실패 메시지 처리를 커스텀 핸들러로 분리합니다.
                        .failureHandler(new FormLoginAuthenticationFailureHandler())
                        .successHandler(roleBasedAuthenticationSuccessHandler)
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/members/logout")
                        .logoutSuccessUrl("/main?logout")
                        .permitAll()
                );

        if (isSocialLoginEnabled()) {
            security.oauth2Login(oauth2 -> oauth2
                    .loginPage("/members/login")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                            .oidcUserService(googleOidcUserService::loadUser)
                    )
                    .successHandler(roleBasedAuthenticationSuccessHandler)
            );
        }

        return security.build();
    }

    private boolean isSocialLoginEnabled() {
        return clientRegistrationRepositoryProvider.getIfAvailable() != null
                && (isGoogleLoginEnabled() || isKakaoLoginEnabled());
    }

    private boolean isGoogleLoginEnabled() {
        return StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id"))
                && StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-secret"));
    }

    private boolean isKakaoLoginEnabled() {
        return StringUtils.hasText(environment.getProperty("spring.security.oauth2.client.registration.kakao.client-id"));
    }
}
