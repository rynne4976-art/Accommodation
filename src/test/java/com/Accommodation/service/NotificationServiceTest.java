package com.Accommodation.service;

import com.Accommodation.constant.Role;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Notification;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.profiles.include=")
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("예약 취소 알림은 주문 상세 URL로 저장된다")
    void sendOrderCancelledStoresOrderDetailTargetUrl() {
        Member member = createMember("user@test.com");
        memberRepository.save(member);

        notificationService.sendOrderCancelled(member.getEmail(), 7L);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification notification = notifications.get(0);
        assertNotNull(notification.getId());
        assertEquals(member.getEmail(), notification.getMember().getEmail());
        assertEquals("예약 #7 이(가) 취소되었습니다.", notification.getMessage());
        assertEquals("/orders/7", notification.getTargetUrl());
        assertFalse(notification.isRead());
    }

    @Test
    @DisplayName("알림 열기는 읽음 처리 후 주문 상세 URL을 반환한다")
    void markAsReadAndGetTargetUrlReturnsOrderDetailUrl() {
        Member member = createMember("user2@test.com");
        memberRepository.save(member);

        notificationService.sendOrderCancelled(member.getEmail(), 15L);
        Notification notification = notificationRepository.findAll().get(0);

        String targetUrl = notificationService.markAsReadAndGetTargetUrl(member.getEmail(), notification.getId());

        Notification updatedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals("/orders/15", targetUrl);
        assertTrue(updatedNotification.isRead());
    }

    private Member createMember(String email) {
        Member member = new Member();
        member.setName("테스트회원");
        member.setEmail(email);
        member.setPassword("{bcrypt}test-password");
        member.setNumber("01012345678");
        member.setAddress("서울시 강남구");
        member.setRole(Role.USER);
        return member;
    }
}
