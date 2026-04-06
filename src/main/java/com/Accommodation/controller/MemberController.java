package com.Accommodation.controller;

import com.Accommodation.dto.MemberFormDto;
import com.Accommodation.dto.MemberUpdateDto;
import com.Accommodation.dto.PasswordChangeDto;
import com.Accommodation.entity.Member;
import com.Accommodation.service.MemberService;
import com.Accommodation.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                              @RequestParam(value = "signupSuccess", required = false) String signupSuccess,
                              Model model) {
        // 로그인 실패와 회원가입 완료 메시지를 같은 로그인 화면에서 처리합니다.
        if (error != null) {
            model.addAttribute("loginErrorMessage", "이메일 또는 비밀번호를 다시 확인해주세요.");
        }
        if (signupSuccess != null) {
            model.addAttribute("signupSuccessMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
        }
        return "member/memberLoginForm";
    }

    @GetMapping("/members/mypage")
    public String myPage(@AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (userDetails == null) {
            return "redirect:/members/login";
        }

        Member member;
        try {
            member = memberService.getMemberByEmail(userDetails.getUsername());
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("loginErrorMessage", "회원 정보를 다시 확인해주세요.");
            return "redirect:/members/login";
        }

        model.addAttribute("member", member);
        model.addAttribute("orders",
                orderService.getOrderList(userDetails.getUsername(), PageRequest.of(0, 20)));
        return "member/mypage";
    }

    @GetMapping("/members/mypage/edit")
    public String editMyPage(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(value = "reservationInfoRequired", required = false) String reservationInfoRequired,
                             Model model) {
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
        model.addAttribute("memberUpdateDto", MemberUpdateDto.from(member));
        if (reservationInfoRequired != null) {
            model.addAttribute("reservationInfoRequiredMessage", "예약을 진행하려면 휴대폰 번호와 주소를 먼저 입력해주세요.");
        }
        return "member/mypageEdit";
    }

    @PostMapping("/members/mypage/edit")
    public String editMyPage(@AuthenticationPrincipal UserDetails userDetails,
                             @Valid MemberUpdateDto memberUpdateDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "member/mypageEdit";
        }

        memberService.updateMember(userDetails.getUsername(), memberUpdateDto);
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
        return "member/passwordEdit";
    }

    @PostMapping("/members/mypage/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid PasswordChangeDto passwordChangeDto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
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
        } catch (IllegalArgumentException e) {
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

        if (bindingResult.hasErrors()) {
            return "member/memberForm";
        }

        try {
            memberService.saveMember(memberFormDto);
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "member/memberForm";
        }

        // PRG 패턴으로 새로고침 시 중복 회원가입 요청이 나가지 않도록 합니다.
        redirectAttributes.addAttribute("signupSuccess", "true");
        return "redirect:/members/login";

    }


}
