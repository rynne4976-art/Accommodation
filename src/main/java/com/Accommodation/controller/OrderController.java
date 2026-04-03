package com.Accommodation.controller;

import com.Accommodation.dto.OrderDto;
import com.Accommodation.dto.OrderUpdateDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.OrderService;
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

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AccomService accomService;

    // ── 예약 폼 페이지 (GET) ──────────────────────────────────────────────────
    @GetMapping("/orders/accom/{accomId}")
    public String orderForm(@PathVariable Long accomId,
                            @RequestParam(required = false) LocalDate checkInDate,
                            @RequestParam(required = false) LocalDate checkOutDate,
                            Model model) {
        Accom accom = accomService.getAccomDtl(accomId);
        model.addAttribute("accom", accom);
        model.addAttribute("checkInDate", checkInDate);
        model.addAttribute("checkOutDate", checkOutDate);
        return "order/orderForm";
    }

    // ── 주문 생성 (AJAX POST) ─────────────────────────────────────────────────
    @PostMapping("/order")
    @ResponseBody
    public ResponseEntity<?> order(@RequestBody @Valid OrderDto orderDto,
                                   BindingResult bindingResult,
                                   Principal principal) {
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
}
