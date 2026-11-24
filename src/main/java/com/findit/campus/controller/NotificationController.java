package com.findit.campus.controller;

import com.findit.campus.entity.AppUser;
import com.findit.campus.service.FindItService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {
    private final FindItService findItService;

    public NotificationController(FindItService findItService) {
        this.findItService = findItService;
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal AppUser user, Model model) {
        model.addAttribute("notifications", findItService.notificationsForUser(user.getId()));
        return "notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable Long id, @AuthenticationPrincipal AppUser user,
                           RedirectAttributes redirectAttributes) {
        try {
            Long reportId = findItService.markNotificationRead(id, user.getId());
            return "redirect:/reports/" + reportId;
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/notifications";
        }
    }
}
