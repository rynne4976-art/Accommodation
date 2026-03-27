package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/accom")
public class AccomController {

    @GetMapping("/reserve")
    public String reserve(){


        return "/accom/reservationStatus";
    }


}
