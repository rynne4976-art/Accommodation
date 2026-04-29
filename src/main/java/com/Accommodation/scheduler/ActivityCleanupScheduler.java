package com.Accommodation.scheduler;

import com.Accommodation.service.RegionActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityCleanupScheduler {

    private final RegionActivityService regionActivityService;

    @Scheduled(cron = "0 10 3 * * *")
    public void removeExpiredEvents() {
        LocalDate today = LocalDate.now();
        int removedCount = regionActivityService.removeExpiredEvents(today);
        log.info("[ActivityCleanupScheduler] 기간 지난 행사 정리 완료 - {}건 (기준일: {})", removedCount, today);
    }
}
