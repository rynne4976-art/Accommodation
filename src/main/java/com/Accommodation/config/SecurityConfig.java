package com.Accommodation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 🧾 SecurityConfig (보안 설정)
 *
 * ▶ 현재 단계에서는 비밀번호 암호화 기능을 사용하기 위한 설정입니다.
 * ▶ 로그인 기능을 본격적으로 붙이기 전까지는 모든 요청을 허용합니다.
 *
 * 현재 역할:
 * - PasswordEncoder Bean 등록
 * - 모든 URL 접근 허용
 * - 기본 로그인 화면 비활성화
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 🔐 비밀번호 암호화 객체 등록
     *
     * BCryptPasswordEncoder:
     * - 비밀번호를 안전하게 암호화
     * - 같은 비밀번호여도 매번 다른 암호문 생성 가능
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 🌐 보안 필터 설정
     *
     * 현재는:
     * - 모든 요청 허용
     * - 기본 로그인 페이지 비활성화
     *
     * 이유:
     * - 아직 로그인 기능 구현 전이기 때문
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/main",
                                "/members/new",
                                "/members/login",
                                "/css/**",
                                "/js/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/members/login")   // 우리가 만든 로그인 페이지
                        .loginProcessingUrl("/members/login")
                        .defaultSuccessUrl("/main", true)
                        .usernameParameter("email")    // 중요 (email로 로그인)
                        .passwordParameter("password")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/main")
                );

        return http.build();
    }
}