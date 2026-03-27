package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/accom")
public class AccomController {

    @GetMapping("/reserve")
    public String reserve(Model model){
        Accom accom = new Accom();
        Member member = new Member();
        model.addAttribute("accom", accom);
        model.addAttribute("member", member);

        return "accom/reservationStatus";
    }


}
