package com.findit.campus.controller;

import com.findit.campus.dto.ProfileForm;
import com.findit.campus.entity.AppUser;
import com.findit.campus.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AppUser user, Model model) {
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setNim(user.getNim() == null ? "" : user.getNim());
        form.setStudyProgram(user.getStudyProgram() == null ? "" : user.getStudyProgram());
        model.addAttribute("profileForm", form);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute ProfileForm profileForm,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal AppUser user,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "profile";
        }
        try {
            accountService.updateProfile(user, profileForm.getFullName(),
                    profileForm.getNim(), profileForm.getStudyProgram());
            redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil diperbarui.");
            return "redirect:/profile";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            return "profile";
        }
    }
}
