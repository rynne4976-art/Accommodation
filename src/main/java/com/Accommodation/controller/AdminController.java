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

    private final AdminService adminService;

    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/members")
    public String memberManage(@RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {
        model.addAttribute("memberPage",
                adminService.getMemberPage(PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "regTime"))));
        return "admin/memberMng";
    }

    @GetMapping("/admin/orders")
    public String orderManage(@RequestParam(value = "page", defaultValue = "0") int page,
                              Model model) {
        model.addAttribute("orderPage",
                adminService.getOrderPage(PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "orderDate"))));
        return "admin/orderMng";
    }
}
