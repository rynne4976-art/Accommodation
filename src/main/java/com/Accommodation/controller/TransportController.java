package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.service.AccomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

@Controller
@RequiredArgsConstructor
public class TransportController {

    private final AccomService accomService;

    /**
     * 교통 페이지
     * - URL 파라미터 accomId 우선
     * - 없으면 최근 본 숙소 쿠키 사용
     */
    @GetMapping("/transport")
    public String transportPage(
            @RequestParam(value = "accomId", required = false) Long accomId,
            @CookieValue(value = "recentViewedAccoms", required = false) String recentViewedCookie,
            Model model
    ) {

        Accom accom = null;

        // 1️⃣ URL 파라미터로 숙소 선택
        if (accomId != null) {
            accom = accomService.getAccomDtl(accomId);
        }

        // 2️⃣ 최근 본 숙소 쿠키에서 자동 선택
        if (accom == null) {
            Long recentAccomId = extractFirstAccomIdFromCookie(recentViewedCookie);

            if (recentAccomId != null) {
                accom = accomService.getAccomDtl(recentAccomId);
            }
        }

        model.addAttribute("accom", accom);

        return "transport/transport";
    }


    /**
     * 최근 본 숙소 쿠키에서 첫 번째 숙소 ID 추출
     */
    private Long extractFirstAccomIdFromCookie(String cookie) {

        if (cookie == null || cookie.isBlank()) {
            return null;
        }

        return Arrays.stream(cookie.split("[,-]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }
}