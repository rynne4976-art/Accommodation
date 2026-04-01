package com.Accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;

@Controller
public class MainController {

    @GetMapping("/")
    public String home(@RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        return renderMain(logout, model);
    }

    @GetMapping("/main")
    public String mainPage(@RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        return renderMain(logout, model);
    }

    private String renderMain(String logout, Model model) {
        model.addAttribute("accomList", new ArrayList<>());
        if (logout != null) {
            model.addAttribute("logoutMessage", "안전하게 로그아웃되었습니다.");
        }
        return "/main";
    }

}
