package com.Accommodation.service;

import com.Accommodation.dto.NotificationDto;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Notification;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationService {

    private static final long DEFAULT_TIMEOUT = 60L * 60L * 1000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               MemberRepository memberRepository) {
        this.notificationRepository = notificationRepository;
        this.memberRepository = memberRepository;
    }

    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(email, emitter);

        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));
        emitter.onError((ex) -> emitters.remove(email));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connected"));
        } catch (IOException e) {
            emitters.remove(email);
            throw new IllegalStateException("알림 연결을 시작할 수 없습니다.", e);
        }

        return emitter;
    }

    public void sendOrderCancelled(String email, Long orderId) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setMember(member);
        notification.setMessage("예약 #" + orderId + " 이(가) 취소되었습니다.");
        notification.setTargetUrl("/orders");
        notification.setRead(false);
        notificationRepository.save(notification);

        SseEmitter emitter = emitters.get(email);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("order-cancelled")
                    .data(new NotificationDto(notification)));
        } catch (IOException e) {
            emitters.remove(email);
            log.warn("Failed to send SSE notification to {}", email, e);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getRecentNotifications(String email) {
        return notificationRepository.findByMemberEmailOrderByRegTimeDesc(email, PageRequest.of(0, 10))
                .stream()
                .map(NotificationDto::new)
                .toList();
    }

    @Transactional
    public void markAsRead(String email, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림 정보를 찾을 수 없습니다."));

        if (!notification.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("알림 처리 권한이 없습니다.");
        }

        notification.setRead(true);
    }

    @Transactional
    public String markAsReadAndGetTargetUrl(String email, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림 정보를 찾을 수 없습니다."));

        if (!notification.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("알림 처리 권한이 없습니다.");
        }

        notification.setRead(true);
        return notification.getTargetUrl();
    }

    /**
     * 만실 알림 – 장바구니에 해당 날짜를 담은 다른 사용자에게 SSE 및 DB 알림 전송
     *
     * @param email     수신 대상 이메일
     * @param accomName 만실이 된 숙소명
     * @param soldOutDate 만실이 된 날짜
     */
    public void sendSoldOutAlert(String email, String accomName, LocalDate soldOutDate) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setMember(member);
        notification.setMessage(
                "'" + accomName + "' " + soldOutDate + " 날짜가 만실되었습니다. "
                + "장바구니에서 해당 항목을 확인해 주세요.");
        notification.setTargetUrl("/cart");
        notification.setRead(false);
        notificationRepository.save(notification);

        SseEmitter emitter = emitters.get(email);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("sold-out")
                    .data(new NotificationDto(notification)));
        } catch (IOException e) {
            emitters.remove(email);
            log.warn("Failed to send sold-out SSE notification to {}", email, e);
        }
    }
}
