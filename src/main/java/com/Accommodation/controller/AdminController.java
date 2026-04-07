package com.Accommodation.controller;

import com.Accommodation.constant.OrderStatus;
import com.Accommodation.constant.Role;
import com.Accommodation.dto.MemberSearchDto;
import com.Accommodation.dto.OrderSearchDto;
import com.Accommodation.exception.AdminException;
import com.Accommodation.service.AdminService;
import com.Accommodation.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminController {

    // 관리자 화면은 조회 책임만 따로 모아 도메인 서비스와 분리합니다.
    private final AdminService adminService;
    private final OrderService orderService;

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("memberCount", adminService.getMemberCount());
        model.addAttribute("accomCount", adminService.getAccomCount());
        model.addAttribute("orderCount", adminService.getOrderCount());
        return "admin/dashboard";
    }

    @GetMapping("/admin/members")
    public String memberManage(@RequestParam(value = "page", defaultValue = "0") int page,
                               MemberSearchDto memberSearchDto,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        // 최신 가입 회원이 먼저 보이도록 가입일 기준 내림차순 정렬을 사용합니다.
        model.addAttribute("memberPage",
                adminService.getMemberPage(
                        memberSearchDto,
                        userDetails.getUsername(),
                        PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "regTime"))
                ));
        model.addAttribute("memberSearchDto", memberSearchDto);
        return "admin/memberMng";
    }

    @GetMapping("/admin/members/{memberId}")
    public String memberDetail(@PathVariable("memberId") Long memberId,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               MemberSearchDto memberSearchDto,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            model.addAttribute("member", adminService.getMemberDetail(memberId));
        } catch (AdminException e) {
            redirectAttributes.addAttribute("page", page);
            redirectAttributes.addAttribute("searchBy", memberSearchDto.getSearchBy());
            redirectAttributes.addAttribute("searchQuery", memberSearchDto.getSearchQuery());
            redirectAttributes.addAttribute("role", memberSearchDto.getRole());
            redirectAttributes.addAttribute("roleError", e.getMessage());
            return "redirect:/admin/members";
        }
        model.addAttribute("page", page);
        model.addAttribute("memberSearchDto", memberSearchDto);
        return "admin/memberDetail";
    }

    @PostMapping("/admin/members/{memberId}/role")
    public String updateMemberRole(@PathVariable("memberId") Long memberId,
                                   @RequestParam("role") Role role,
                                   @RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "searchBy", required = false) String searchBy,
                                   @RequestParam(value = "searchQuery", required = false) String searchQuery,
                                   @RequestParam(value = "filterRole", required = false) Role filterRole,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            adminService.updateMemberRole(memberId, role, userDetails.getUsername());
            redirectAttributes.addAttribute("roleUpdated", "true");
        } catch (AdminException e) {
            redirectAttributes.addAttribute("roleError", e.getMessage());
        }

        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("searchBy", searchBy);
        redirectAttributes.addAttribute("searchQuery", searchQuery);
        redirectAttributes.addAttribute("role", filterRole);
        return "redirect:/admin/members";
    }

    @GetMapping("/admin/orders")
    public String orderManage(@RequestParam(value = "page", defaultValue = "0") int page,
                              OrderSearchDto orderSearchDto,
                              Model model) {
        // 최신 예약이 먼저 보이도록 예약일 기준 내림차순 정렬을 사용합니다.
        model.addAttribute("orderPage",
                adminService.getOrderPage(
                        orderSearchDto,
                        PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "orderDate"))
                ));
        model.addAttribute("orderSearchDto", orderSearchDto);
        return "admin/orderMng";
    }

    @PostMapping("/admin/orders/{orderId}/status")
    public String updateOrderStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam("status") OrderStatus status,
                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                    @RequestParam(value = "searchBy", required = false) String searchBy,
                                    @RequestParam(value = "searchQuery", required = false) String searchQuery,
                                    @RequestParam(value = "filterOrderStatus", required = false) OrderStatus filterOrderStatus,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatusByAdmin(orderId, status);
            redirectAttributes.addAttribute("orderUpdated", "true");
        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            redirectAttributes.addAttribute(
                    "orderError",
                    StringUtils.hasText(e.getMessage()) ? e.getMessage() : "예약 정보를 찾을 수 없습니다."
            );
        }

        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("searchBy", searchBy);
        redirectAttributes.addAttribute("searchQuery", searchQuery);
        redirectAttributes.addAttribute("orderStatus", filterOrderStatus);
        return "redirect:/admin/orders";
    }
}
