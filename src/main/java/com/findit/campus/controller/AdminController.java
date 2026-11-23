package com.findit.campus.controller;

import com.findit.campus.entity.AppUser;
import com.findit.campus.entity.UserRole;
import com.findit.campus.service.AdminService;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final SessionRegistry sessionRegistry;

    public AdminController(AdminService adminService, SessionRegistry sessionRegistry) {
        this.adminService = adminService;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        var reports = adminService.reports();
        model.addAttribute("users", adminService.users());
        model.addAttribute("reports", reports);
        model.addAttribute("userCount", adminService.userCount());
        model.addAttribute("reportCount", reports.size());
        model.addAttribute("pendingClaimCount", adminService.pendingClaimCount());
        model.addAttribute("roles", UserRole.values());
        return "admin";
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam UserRole role,
                             @AuthenticationPrincipal AppUser admin,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.updateRole(id, role, admin.getEmail());
            expireUserSessions(id);
            redirectAttributes.addFlashAttribute("successMessage", "Role pengguna berhasil diperbarui.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#pengguna";
    }

    private void expireUserSessions(Long userId) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof AppUser user && user.getId().equals(userId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }

    @PostMapping("/admin/reports/{id}/delete")
    public String deleteReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteReport(id);
            redirectAttributes.addFlashAttribute("successMessage", "Laporan dan klaim terkait berhasil dihapus.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#laporan";
    }
}
