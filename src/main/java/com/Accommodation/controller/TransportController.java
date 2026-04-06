package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.repository.CarListRepository;
import com.Accommodation.service.AccomService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TransportController {

    private final CarListRepository carListRepository;
    private final AccomService accomService;

    @Value("${naver.map.client.id}")
    private String naverMapClientId;

    @GetMapping("/transport")
    public String transportPage(@RequestParam(value = "accomId") Optional<Long> accomId,
                                Model model) {
        model.addAttribute("compactHeader", true);
        model.addAttribute("carList", carListRepository.findTop3ByOrderByCarPriceAsc());
        model.addAttribute("naverMapClientId", naverMapClientId);

        if (accomId.isPresent()) {
            try {
                Accom accom = accomService.getAccomDtl(accomId.get());
                model.addAttribute("selectedAccom", accom);
            } catch (EntityNotFoundException ignored) {
                model.addAttribute("selectedAccom", null);
            }
        }

        return "transport/transport";
    }
}
