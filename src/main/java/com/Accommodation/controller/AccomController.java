package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/accom")
public class AccomController {

    @GetMapping("/create")
    public String create(Model model) {

        Accom newAccom = new Accom();
        model.addAttribute("newAccom", newAccom);
        return "accom/booking";
    }
    @PostMapping("/create")
    public String insert(){
        return "accom/booking";
    }

    @GetMapping("/reserve")
    public String reserve(Model model){
        Accom accom = new Accom();
        Member member = new Member();
        model.addAttribute("accom", accom);
        model.addAttribute("member", member);

        return "accom/bookingStatus";
    }

    @GetMapping("/delete")
    public String delete(){



        return "redirect:/booking";
    }


}
