package com.Accommodation.controller;

import com.Accommodation.dto.OrderDto;
import com.Accommodation.dto.OrderUpdateDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomOperationDay;
import com.Accommodation.entity.AccomOperationPolicy;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.MemberService;
import com.Accommodation.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AccomService accomService;
    private final MemberService memberService;

    // ── 예약 폼 페이지 (GET) ──────────────────────────────────────────────────
    @GetMapping("/orders/accom/{accomId}")
    public String orderForm(@PathVariable Long accomId,
                            @RequestParam(required = false) LocalDate checkInDate,
                            @RequestParam(required = false) LocalDate checkOutDate,
                            @RequestParam(required = false, defaultValue = "1") Integer adultCount,
                            @RequestParam(required = false, defaultValue = "0") Integer childCount,
                            @RequestParam(required = false, defaultValue = "1") Integer roomCount,
                            @RequestParam(required = false) Long cartItemId,
                            Principal principal,
                            Model model) {
        if (!memberService.hasRequiredReservationInfo(principal.getName())) {
            return "redirect:/members/mypage/edit?reservationInfoRequired=true";
        }

        Accom accom = accomService.getAccomDtl(accomId);
        AccomOperationPolicy policy = accom.getOperationPolicy();

        List<String> operationDays = accom.getOperationDayList() == null
                ? Collections.emptyList()
                : accom.getOperationDayList().stream()
                .map(AccomOperationDay::getOperationDate)
                .map(LocalDate::toString)
                .toList();

        List<String> soldOutDays = orderService.getSoldOutDates(accomId).stream()
                .map(LocalDate::toString)
                .toList();

            model.addAttribute("accom", accom);
            model.addAttribute("checkInDate", checkInDate);
            model.addAttribute("checkOutDate", checkOutDate);
            model.addAttribute("adultCount", adultCount);
            model.addAttribute("childCount", childCount);
            model.addAttribute("roomCount", roomCount);
            model.addAttribute("cartItemId", cartItemId);
            model.addAttribute("isEditingCart", cartItemId != null);
            model.addAttribute("operationDays", operationDays);
            model.addAttribute("soldOutDays", soldOutDays);
            model.addAttribute("operationStartDate", policy != null ? policy.getOperationStartDate() : null);
            model.addAttribute("operationEndDate", policy != null ? policy.getOperationEndDate() : null);
            model.addAttribute("checkInTime", policy != null ? policy.getCheckInTime() : null);
            model.addAttribute("checkOutTime", policy != null ? policy.getCheckOutTime() : null);
            model.addAttribute("backPath", cartItemId != null ? "/cart" : "/accom/" + accom.getId());

        return "order/orderForm";
    }

    @GetMapping("/orders/accom/{accomId}/availability")
    @ResponseBody
    public ResponseEntity<?> getAvailability(@PathVariable Long accomId,
                                             @RequestParam LocalDate checkInDate,
                                             @RequestParam LocalDate checkOutDate) {
        try {
            int remaining = orderService.getRemainingRooms(accomId, checkInDate, checkOutDate);
            return new ResponseEntity<>(Map.of("remainingRooms", remaining), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/orders/accom/{accomId}/booking-meta")
    @ResponseBody
    public ResponseEntity<?> getBookingMeta(@PathVariable Long accomId) {
        try {
            Accom accom = accomService.getAccomDtl(accomId);
            AccomOperationPolicy policy = accom.getOperationPolicy();

            List<String> operationDays = accom.getOperationDayList() == null
                    ? Collections.emptyList()
                    : accom.getOperationDayList().stream()
                    .map(AccomOperationDay::getOperationDate)
                    .map(LocalDate::toString)
                    .toList();

            List<String> soldOutDays = orderService.getSoldOutDates(accomId).stream()
                    .map(LocalDate::toString)
                    .toList();

            Map<String, Object> payload = new HashMap<>();
            payload.put("operationDays", operationDays);
            payload.put("soldOutDays", soldOutDays);
            payload.put("operationStartDate", policy != null ? policy.getOperationStartDate() : null);
            payload.put("operationEndDate", policy != null ? policy.getOperationEndDate() : null);
            payload.put("checkInTime", formatTime(policy != null ? policy.getCheckInTime() : null));
            payload.put("checkOutTime", formatTime(policy != null ? policy.getCheckOutTime() : null));

            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 월별 날짜별 잔여 객실 수 조회 (캘린더 badge 표시용)
     * GET /orders/accom/{accomId}/monthly-availability?year=2026&month=4
     * → { "2026-04-01": 3, "2026-04-05": 0, ... }  (운영일만 포함)
     */
    @GetMapping("/orders/accom/{accomId}/monthly-availability")
    @ResponseBody
    public ResponseEntity<?> getMonthlyAvailability(@PathVariable Long accomId,
                                                     @RequestParam int year,
                                                     @RequestParam int month) {
        try {
            Map<String, Integer> availability = orderService.getMonthlyAvailability(accomId, year, month);
            return new ResponseEntity<>(availability, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private String formatTime(LocalTime localTime) {
        return localTime != null ? localTime.toString() : "";
    }

    // ── 주문 생성 (AJAX POST) ─────────────────────────────────────────────────
    @PostMapping("/order")
    @ResponseBody
    public ResponseEntity<?> order(@RequestBody @Valid OrderDto orderDto,
                                   BindingResult bindingResult,
                                   Principal principal) {
        if (!memberService.hasRequiredReservationInfo(principal.getName())) {
            return new ResponseEntity<>("예약을 위해 연락처와 주소를 먼저 입력해주세요.", HttpStatus.BAD_REQUEST);
        }

        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                sb.append(error.getDefaultMessage());
            }
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        Long orderId;
        try {
            orderId = orderService.order(orderDto, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    // ── 주문 취소 (AJAX POST) ─────────────────────────────────────────────────
    @PostMapping("/order/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId,
                                         Principal principal) {
        try {
            orderService.cancelOrder(orderId, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    // ── 주문 수정 (AJAX PUT) ──────────────────────────────────────────────────
    @PutMapping("/order/{orderId}")
    @ResponseBody
    public ResponseEntity<?> updateOrder(@PathVariable Long orderId,
                                         @RequestBody @Valid OrderUpdateDto dto,
                                         BindingResult bindingResult,
                                         Principal principal) {
        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                sb.append(error.getDefaultMessage());
            }
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        try {
            orderService.updateOrder(orderId, dto, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    // ── 주문 내역 페이지 (GET) ────────────────────────────────────────────────
    @GetMapping("/orders")
    public String orderHist(Model model, Principal principal) {
        Pageable pageable = PageRequest.of(0, 5);
        model.addAttribute("orders",
                orderService.getOrderList(principal.getName(), pageable));
        return "order/orderHist";
    }

    @GetMapping("/orders/{orderId}")
    public String orderDetail(@PathVariable Long orderId,
                              Model model,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("order", orderService.getOrderDetail(orderId, principal.getName()));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("orderErrorMessage", "요청한 예약 정보를 찾을 수 없습니다.");
            return "redirect:/orders";
        }

        return "order/orderDetail";
    }
}
