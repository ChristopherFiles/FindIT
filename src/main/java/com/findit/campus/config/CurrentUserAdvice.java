package com.findit.campus.config;

import com.findit.campus.entity.AppUser;
import com.findit.campus.service.FindItService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class CurrentUserAdvice {
    private final FindItService findItService;

    public CurrentUserAdvice(FindItService findItService) {
        this.findItService = findItService;
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUser user)) {
            return null;
        }
        return user;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount() {
        AppUser user = currentUser();
        return user == null ? 0 : findItService.unreadNotificationCount(user.getId());
    }
}
