package com.Accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WishController {

    @GetMapping("/wish")
    public String wishList(Model model) {
        model.addAttribute("compactHeader", true);
        model.addAttribute("wishCount", 0);
        return "wish/wishList";
    }
}
