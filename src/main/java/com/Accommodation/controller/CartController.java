package com.Accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class CartController {

    @GetMapping("/cart")
    public String cartList(Model model, Principal principal) {
        model.addAttribute("compactHeader", true);
        model.addAttribute("cartCount", 0);
        model.addAttribute("cartTotalPrice", 0);
        return "cart/cartList";
    }
}
