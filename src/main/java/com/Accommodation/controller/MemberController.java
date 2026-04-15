package com.Accommodation.controller;

import com.Accommodation.dto.MemberFormDto;
import com.Accommodation.dto.MemberUpdateDto;
import com.Accommodation.dto.OrderHistPage;
import com.Accommodation.dto.PasswordChangeDto;
import com.Accommodation.entity.Member;
import com.Accommodation.exception.MemberException;
import com.Accommodation.service.MemberService;
import com.Accommodation.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * 🧾 MemberController (회원 컨트롤러)
 *
 * ▶ 사용자 요청(URL)을 받아서 화면(View)으로 연결합니다.
 *
 * 현재 기능:
 * - 회원가입 페이지 보여주기
 * - 회원가입 처리
 */
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final OrderService orderService;

    /**
     * 📄 회원가입 페이지 요청
     *
     * URL: /members/new
     *
     * 역할:
     * - 빈 DTO 생성
     * - 화면에 전달
     */
    @GetMapping("/members/new")
    public String memberForm(Model model) {

        // 빈 DTO 생성 후 화면으로 전달
        model.addAttribute("memberFormDto", new MemberFormDto());

        // templates/member/memberForm.html 반환
        return "member/memberForm";

    }

    /**
     * 🔐 로그인 페이지 요청
     *
     * URL: /members/login
     *
     * 역할:
     * - 로그인 화면을 사용자에게 보여줌
     */
    @GetMapping("/members/login")
    public String memberLogin(@RequestParam(value = "error", required = false) String error,
                              @RequestParam(value = "socialError", required = false) String socialError,
                              @RequestParam(value = "signupSuccess", required = false) String signupSuccess,
                              @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                              HttpSession session,
                              Model model) {
        // 로그인 실패와 회원가입 완료 메시지를 같은 로그인 화면에서 처리합니다.
        if (error != null) {
            model.addAttribute("loginErrorMessage", "이메일 또는 비밀번호를 다시 확인해주세요.");
        }
        if (socialError != null) {
            model.addAttribute("loginErrorMessage", "소셜 로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
        if (signupSuccess != null) {
            model.addAttribute("signupSuccessMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
        }
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            session.setAttribute("redirectUrl", redirectUrl);
        }
        model.addAttribute("redirectUrl", redirectUrl);
        return "member/memberLoginForm";
    }

    @GetMapping("/members/check-email")
    @ResponseBody
    public Map<String, Object> checkEmailDuplicate(@RequestParam("email") String email) {
        return Map.of("duplicate", memberService.existsByEmail(email));
    }

    @GetMapping("/members/mypage")
    public String myPage(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam(required = false) String period,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(defaultValue = "예약완료") String status,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (userDetails == null) {
            return "redirect:/members/login";
        }

        Member member;
        try {
            member = memberService.getMemberByEmail(userDetails.getUsername());
        } catch (MemberException e) {
            redirectAttributes.addFlashAttribute("loginErrorMessage", "회원 정보를 다시 확인해주세요.");
            return "redirect:/members/login";
        }

        LocalDate today = LocalDate.now();
        LocalDate effectiveFrom;
        LocalDate effectiveTo;
        String effectivePeriod;

        if (from != null && to != null) {
            effectiveFrom   = from;
            effectiveTo     = to;
            effectivePeriod = "";
        } else {
            effectiveTo = today;
            if ("1m".equals(period)) {
                effectiveFrom   = today.minusMonths(1);
                effectivePeriod = "1m";
            } else if ("3m".equals(period)) {
                effectiveFrom   = today.minusMonths(3);
                effectivePeriod = "3m";
            } else if ("12m".equals(period)) {
                effectiveFrom   = today.minusMonths(12);
                effectivePeriod = "12m";
            } else if ("all".equals(period)) {
                effectiveFrom   = LocalDate.of(2000, 1, 1);
                effectiveTo     = LocalDate.of(2099, 12, 31);
                effectivePeriod = "all";
            } else {
                effectiveFrom   = today.minusWeeks(1);
                effectivePeriod = "1w";
            }
        }

        OrderHistPage result = orderService.getOrderHistPage(
                userDetails.getUsername(),
                LocalDateTime.of(effectiveFrom, LocalTime.MIN),
                LocalDateTime.of(effectiveTo, LocalTime.MAX),
                status, page, 5
        );

        model.addAttribute("member", member);
        model.addAttribute("orders",         result.orders());
        model.addAttribute("totalPages",     result.totalPages());
        model.addAttribute("currentPage",    result.currentPage());
        model.addAttribute("totalCount",     result.totalCount());
        model.addAttribute("selectedPeriod", effectivePeriod);
        model.addAttribute("fromDate",       "all".equals(effectivePeriod) ? "" : effectiveFrom.toString());
        model.addAttribute("toDate",         "all".equals(effectivePeriod) ? "" : effectiveTo.toString());
        model.addAttribute("selectedStatus", status);
        return "member/mypage";
    }

    @GetMapping("/members/mypage/edit")
    public String editMyPage(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(value = "reservationInfoRequired", required = false) String reservationInfoRequired,
                             Model model) {
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
        model.addAttribute("memberUpdateDto", MemberUpdateDto.from(member));
        model.addAttribute("accountEmail", member.getEmail());
        model.addAttribute("socialMember", member.isSocialMember());
        if (reservationInfoRequired != null) {
            model.addAttribute("reservationInfoRequiredMessage", "예약을 진행하려면 휴대폰 번호와 주소를 먼저 입력해주세요.");
        }
        return "member/mypageEdit";
    }

    @PostMapping("/members/mypage/edit")
    public String editMyPage(@AuthenticationPrincipal UserDetails userDetails,
                             @Valid MemberUpdateDto memberUpdateDto,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
        model.addAttribute("accountEmail", member.getEmail());
        model.addAttribute("socialMember", member.isSocialMember());

        if (!member.isSocialMember() && !StringUtils.hasText(memberUpdateDto.getCurrentPassword())) {
            bindingResult.rejectValue("currentPassword", "currentPassword.required", "정보를 수정하려면 현재 비밀번호를 입력해주세요.");
        }

        if (bindingResult.hasErrors()) {
            return "member/mypageEdit";
        }

        try {
            memberService.updateMember(userDetails.getUsername(), memberUpdateDto);
        } catch (MemberException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "member/mypageEdit";
        }

        redirectAttributes.addAttribute("profileUpdated", "true");
        return "redirect:/members/mypage";
    }

    @GetMapping("/members/mypage/password")
    public String changePasswordForm(@AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
        if (member.isSocialMember()) {
            redirectAttributes.addFlashAttribute("profileUpdatedMessage", "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다.");
            return "redirect:/members/mypage";
        }
        model.addAttribute("passwordChangeDto", new PasswordChangeDto());
        model.addAttribute("accountEmail", member.getEmail());
        return "member/passwordEdit";
    }

    @PostMapping("/members/mypage/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid PasswordChangeDto passwordChangeDto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
        model.addAttribute("accountEmail", member.getEmail());
        if (member.isSocialMember()) {
            redirectAttributes.addFlashAttribute("profileUpdatedMessage", "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다.");
            return "redirect:/members/mypage";
        }

        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "새 비밀번호 확인이 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "member/passwordEdit";
        }

        try {
            memberService.changePassword(userDetails.getUsername(), passwordChangeDto);
        } catch (MemberException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "member/passwordEdit";
        }

        redirectAttributes.addAttribute("passwordUpdated", "true");
        return "redirect:/members/mypage";
    }

    /**
     * 🛠 회원가입 처리
     *
     * URL: /members/new
     * Method: POST
     *
     * 역할:
     * 1. 사용자가 입력한 회원가입 값 받기
     * 2. 유효성 검사 확인
     * 3. Service로 회원 저장 요청
     * 4. 성공 시 메인 페이지로 이동
     * 5. 실패 시 에러 메시지와 함께 회원가입 페이지로 복귀
     */
    @PostMapping("/members/new")
    public String memberForm(@Valid MemberFormDto memberFormDto,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasFieldErrors("password")
                && memberFormDto.getPassword() != null
                && memberFormDto.getConfirmPassword() != null
                && !memberFormDto.getPassword().equals(memberFormDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "비밀번호가 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "member/memberForm";
        }

        try {
            memberService.saveMember(memberFormDto);
        } catch (MemberException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "member/memberForm";
        }

        // PRG 패턴으로 새로고침 시 중복 회원가입 요청이 나가지 않도록 합니다.
        redirectAttributes.addAttribute("signupSuccess", "true");
        return "redirect:/members/login";

    }


}
