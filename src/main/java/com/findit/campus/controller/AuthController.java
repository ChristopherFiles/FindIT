package com.findit.campus.controller;

import com.findit.campus.dto.RegistrationForm;
import com.findit.campus.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationForm registrationForm,
                           BindingResult bindingResult, Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            accountService.register(registrationForm.getFullName(), registrationForm.getEmail(),
                    registrationForm.getPassword());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("registrationError", exception.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Akun berhasil dibuat. Silakan masuk.");
        return "redirect:/login";
    }
}
