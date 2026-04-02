package com.Accommodation.config;

import com.Accommodation.constant.Role;
import com.Accommodation.entity.Member;
import com.Accommodation.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email:admin@accom.com}")
    private String adminEmail;

    @Value("${admin.bootstrap.password:Admin1234!}")
    private String adminPassword;

    @Value("${admin.bootstrap.name:관리자}")
    private String adminName;

    @Value("${admin.bootstrap.number:01000000000}")
    private String adminNumber;

    @Value("${admin.bootstrap.address:관리자 기본 주소}")
    private String adminAddress;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.findByEmail(adminEmail) != null) {
            return;
        }

        Member admin = new Member();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNumber(adminNumber);
        admin.setAddress(adminAddress);
        admin.setRole(Role.ADMIN);

        memberRepository.save(admin);
    }
}
