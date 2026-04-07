package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.repository.CarListRepository;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.TransportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TransportController {

    private final CarListRepository carListRepository;
    private final AccomService accomService;
    private final TransportService transportService;

    @GetMapping("/transport")
    public String transportPage(@RequestParam(value = "accomId", required = false) Long accomId,
                                Model model) {

        model.addAttribute("compactHeader", true);
        model.addAttribute("carList", carListRepository.findAllByOrderByCarPriceAsc());
        model.addAttribute("directionsEnabled", transportService.isDirectionsEnabled());

        if (accomId != null) {
            try {
                Accom accom = accomService.getAccomDtl(accomId);
                model.addAttribute("selectedAccom", accom);
            } catch (EntityNotFoundException e) {
                model.addAttribute("selectedAccom", null);
            }
        }

        return "transport/transport";
    }

    /**
     * 지도에서 찍은 좌표 그대로 사용
     */
    @GetMapping("/transport/geocode")
    @ResponseBody
    public ResponseEntity<?> geocode(@RequestParam double lat,
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
     * 좌표 → 좌표 이동 계산
     */
    @GetMapping("/transport/directions/driving")
    @ResponseBody
    public ResponseEntity<?> drivingRoute(@RequestParam double startLat,
                                          @RequestParam double startLng,
                                          @RequestParam double goalLat,
                                          @RequestParam double goalLng,
                                          @RequestParam(defaultValue = "출발지") String source) {

        try {
            return ResponseEntity.ok(
                    transportService.getDrivingRoute(startLat, startLng, goalLat, goalLng, source)
            );

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}