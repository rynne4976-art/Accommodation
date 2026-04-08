package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.repository.CarListRepository;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.TransportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TransportController {

    private final CarListRepository carListRepository;
    private final AccomService accomService;
    private final TransportService transportService;

    /* 네이버 지도 API KEY */
    @Value("${naver.map.client.id}")
    private String naverMapClientId;

    /**
     * 교통 페이지
     */
    @GetMapping("/transport")
    public String transportPage(
            @RequestParam(value="accomId", required=false) Long accomId,
            @CookieValue(value = "recentViewedAccoms", required = false) String recentViewedCookie,
            Model model)
    {

        Accom accom = null;

        // 1) 쿼리스트링으로 accomId가 들어오면 해당 숙소 사용
        if(accomId != null){
            accom = accomService.getAccomDtl(accomId);
        } else {
            // 2) accomId가 없으면 최근 본 숙소 쿠키의 첫 번째 ID를 자동 사용
            Long recentAccomId = extractFirstAccomIdFromCookie(recentViewedCookie);
            if (recentAccomId != null) {
                accom = accomService.getAccomDtl(recentAccomId);
            }
        }

        model.addAttribute("selectedAccom", accom);
        model.addAttribute("accom", accom);
        model.addAttribute("naverMapClientId", naverMapClientId);

        return "transport/transport";
    }

    private Long extractFirstAccomIdFromCookie(String recentViewedCookie) {
        if (recentViewedCookie == null || recentViewedCookie.isBlank()) {
            return null;
        }

        return Arrays.stream(recentViewedCookie.split("[,-]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
    }

    /**
     * 지도 좌표 정보 전달
     */
    @GetMapping("/transport/geocode")
    @ResponseBody
    public ResponseEntity<?> geocode(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String address) {

        try {

            Map<String, Object> result = Map.of(
                    "lat", lat,
                    "lng", lng,
                    "address", address
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        }
    }

    /**
     * 자동차 길찾기
     * 프론트에서 넘긴 출발/도착 좌표를 받아
     * 거리/시간/경로(path)를 JSON으로 응답합니다.
     */
    @GetMapping("/transport/directions/driving")
    @ResponseBody
    public ResponseEntity<?> drivingRoute(
            @RequestParam double startLat,
            @RequestParam double startLng,
            @RequestParam double goalLat,
            @RequestParam double goalLng,
            @RequestParam(defaultValue = "출발지") String source) {

        try {

            return ResponseEntity.ok(
                    transportService.getDrivingRoute(
                            startLat,
                            startLng,
                            goalLat,
                            goalLng,
                            source
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        }
    }

    /**
     * 주변 지하철 검색
     * 현재 위치 기준 최근접 지하철역 정보를 반환합니다.
     */
    @GetMapping("/api/transport/subway")
    @ResponseBody
    public Map<String, Object> subway(
            @RequestParam double lat,
            @RequestParam double lng) {

        return transportService.findNearbySubway(lat, lng);

    }

    /**
     * 주변 버스 정류장 검색
     * 현재 위치 기준 최근접 버스정류장 정보를 반환합니다.
     */
    @GetMapping("/api/transport/bus")
    @ResponseBody
    public Map<String, Object> bus(
            @RequestParam double lat,
            @RequestParam double lng) {

        return transportService.findNearbyBus(lat, lng);

    }

}