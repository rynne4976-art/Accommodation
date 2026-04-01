package com.Accommodation.controller;

import com.Accommodation.dto.OrderDto;
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
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성 (AJAX)
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

    // 주문 취소 (AJAX)
    @PostMapping("/order/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, Principal principal) {
        try {
            orderService.cancelOrder(orderId, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    // 주문 내역 페이지
    @GetMapping("/orders")
    public String orderHist(Model model, Principal principal) {
        Pageable pageable = PageRequest.of(0, 5);
        model.addAttribute("orders", orderService.getOrderList(principal.getName(), pageable));
        return "order/orderHist";
    }
}
