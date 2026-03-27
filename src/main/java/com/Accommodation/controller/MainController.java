package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/main")
    public String mainPage(Model model) {
        Accom accom = new Accom();

        model.addAttribute("accom", accom);


        return "/main";
    }

}