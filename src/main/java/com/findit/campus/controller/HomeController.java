package com.findit.campus.controller;

import com.findit.campus.service.FindItService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final FindItService findItService;

    public HomeController(FindItService findItService) {
        this.findItService = findItService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("reports", findItService.latestReports());
        model.addAttribute("reportCount", findItService.countReports());
        model.addAttribute("foundCount", findItService.countFound());
        return "index";
    }
}
