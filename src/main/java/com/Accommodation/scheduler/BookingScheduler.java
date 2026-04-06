package com.Accommodation.scheduler;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.entity.OrderItem;
import com.Accommodation.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 상태 자동 전환 스케줄러
 *
 * <p>매일 11:00 AM 에 실행되며, 체크아웃 날짜가 오늘 이하이고
 * 아직 CONFIRMED 상태인 OrderItem 을 COMPLETED 로 일괄 전환한다.</p>
 *
 * <pre>
 *   대상: bookingStatus = CONFIRMED AND checkOutDate <= today
 *   결과: bookingStatus → COMPLETED
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduler {

    private final OrderItemRepository orderItemRepository;

    /**
     * 매일 오전 11:00 실행 (체크아웃 시간 기준)
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 11 * * *")
    @Transactional
    public void completeExpiredBookings() {
        LocalDate today = LocalDate.now();

        List<OrderItem> targets = orderItemRepository
                .findAllByBookingStatusAndCheckOutDateBefore(BookingStatus.CONFIRMED, today);

        if (targets.isEmpty()) {
            log.info("[BookingScheduler] 이용 완료 처리 대상 없음 (기준일: {})", today);
            return;
        }

        int count = 0;
        for (OrderItem item : targets) {
            try {
                item.complete();   // FSM: CONFIRMED → COMPLETED
                count++;
            } catch (IllegalStateException e) {
                // 이미 상태가 바뀐 경우 등 예외 발생 시 로그 후 계속 진행
                log.warn("[BookingScheduler] OrderItem #{} 상태 전환 실패: {}",
                        item.getId(), e.getMessage());
            }
        }

        log.info("[BookingScheduler] 이용 완료 처리 완료 - {}건 (기준일: {})", count, today);
    }
}
