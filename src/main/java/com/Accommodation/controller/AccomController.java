package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.NewAccom;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/accom")
public class AccomController {

    @PostMapping("/create/{id}")
    public String create(Model model){

        NewAccom newAccom = new NewAccom();
        model.addAttribute("newAccom", newAccom);


        return "accom/booking";
    }

    @GetMapping("/booking")
    public String reserve(Model model){
        Accom accom = new Accom();
        Member member = new Member();
        model.addAttribute("accom", accom);
        model.addAttribute("member", member);

        return "accom/bookingStatus";
    }

    @GetMapping("/delete/{id}")
    public String delete(){



        return "accom/booking";
    }


}
