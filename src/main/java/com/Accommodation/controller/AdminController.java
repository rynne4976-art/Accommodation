package com.Accommodation.controller;

import com.Accommodation.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminController {

    // 관리자 화면은 조회 책임만 따로 모아 도메인 서비스와 분리합니다.
    private final AdminService adminService;

    @GetMapping("/admin/members")
    public String memberManage(@RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {
        // 최신 가입 회원이 먼저 보이도록 가입일 기준 내림차순 정렬을 사용합니다.
        model.addAttribute("memberPage",
                adminService.getMemberPage(PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "regTime"))));
        return "admin/memberMng";
    }

    @GetMapping("/admin/orders")
    public String orderManage(@RequestParam(value = "page", defaultValue = "0") int page,
                              Model model) {
        // 최신 예약이 먼저 보이도록 예약일 기준 내림차순 정렬을 사용합니다.
        model.addAttribute("orderPage",
                adminService.getOrderPage(PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "orderDate"))));
        return "admin/orderMng";
    }
}
