package com.Accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * AuditConfig
 *
 * @EnableJpaAuditing 을 메인 클래스에서 분리한 설정 파일입니다.
 * 메인 클래스에 두면 슬라이스 테스트(@WebMvcTest 등) 시 충돌이 발생할 수 있습니다.
 *
 * auditorAwareRef = "auditorAware" 로 AuditorAwareImpl 빈을 연결합니다.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}
