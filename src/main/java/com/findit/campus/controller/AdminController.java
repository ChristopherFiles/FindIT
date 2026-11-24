package com.findit.campus.controller;

import com.findit.campus.dto.AdminUserForm;
import com.findit.campus.entity.AppUser;
import com.findit.campus.entity.LostFoundReport;
import com.findit.campus.entity.UserRole;
import com.findit.campus.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        LocalDate today = LocalDate.now();
        addChartSeries(model, "daily", reports, today.minusDays(6), 7,
                date -> date.format(DateTimeFormatter.ofPattern("dd MMM")));
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int offset = 11; offset >= 0; offset--) {
            months.add(currentMonth.minusMonths(offset));
        }
        addMonthSeries(model, reports, months);
        int currentYear = today.getYear();
        List<Integer> years = new ArrayList<>();
        for (int year = currentYear - 4; year <= currentYear; year++) {
            years.add(year);
        }
        addYearSeries(model, reports, years);
        return "admin";
    }

    @GetMapping("/admin/users/new")
    public String newUser(Model model) {
        model.addAttribute("userForm", new AdminUserForm());
        model.addAttribute("editMode", false);
        model.addAttribute("roles", UserRole.values());
        return "admin-user-form";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editUser(@PathVariable Long id, @AuthenticationPrincipal AppUser admin, Model model) {
        AppUser user = adminService.user(id);
        if (user.getId().equals(admin.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Gunakan halaman profil untuk mengubah data akun admin yang sedang digunakan.");
        }
        model.addAttribute("userForm", formFrom(user));
        model.addAttribute("userId", id);
        model.addAttribute("editMode", true);
        model.addAttribute("roles", UserRole.values());
        return "admin-user-form";
    }

    @PostMapping("/admin/users")
    public String createUser(@Valid @ModelAttribute("userForm") AdminUserForm form,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", false);
            model.addAttribute("roles", UserRole.values());
            return "admin-user-form";
        }
        try {
            adminService.createUser(form);
            redirectAttributes.addFlashAttribute("successMessage", "Pengguna berhasil ditambahkan.");
            return "redirect:/admin#pengguna";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            model.addAttribute("editMode", false);
            model.addAttribute("roles", UserRole.values());
            return "admin-user-form";
        }
    }

    @PostMapping("/admin/users/{id}")
    public String updateUser(@PathVariable Long id, @Valid @ModelAttribute("userForm") AdminUserForm form,
                             BindingResult bindingResult, @AuthenticationPrincipal AppUser admin,
                             Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("editMode", true);
            model.addAttribute("roles", UserRole.values());
            return "admin-user-form";
        }
        try {
            adminService.updateUser(id, form, admin.getEmail());
            expireUserSessions(id);
            redirectAttributes.addFlashAttribute("successMessage", "Data pengguna berhasil diperbarui.");
            return "redirect:/admin#pengguna";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("editMode", true);
            model.addAttribute("roles", UserRole.values());
            return "admin-user-form";
        }
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, @AuthenticationPrincipal AppUser admin,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id, admin.getEmail());
            expireUserSessions(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pengguna berhasil dihapus.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#pengguna";
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

    private AdminUserForm formFrom(AppUser user) {
        AdminUserForm form = new AdminUserForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setNim(user.getNim() == null ? "" : user.getNim());
        form.setStudyProgram(user.getStudyProgram() == null ? "" : user.getStudyProgram());
        form.setRole(user.getRole());
        return form;
    }

    private void addChartSeries(Model model, String prefix, List<LostFoundReport> reports,
                                LocalDate start, int count,
                                Function<LocalDate, String> labelFormatter) {
        List<LocalDate> dates = new ArrayList<>();
        for (int offset = 0; offset < count; offset++) {
            dates.add(start.plusDays(offset));
        }
        Map<LocalDate, Long> totals = reports.stream()
                .map(report -> report.getCreatedAt().toLocalDate())
                .filter(dates::contains)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        model.addAttribute(prefix + "Labels", dates.stream().map(labelFormatter).toList());
        model.addAttribute(prefix + "Counts", dates.stream().map(date -> totals.getOrDefault(date, 0L)).toList());
    }

    private void addMonthSeries(Model model, List<LostFoundReport> reports, List<YearMonth> months) {
        Map<YearMonth, Long> totals = reports.stream()
                .map(report -> YearMonth.from(report.getCreatedAt()))
                .filter(months::contains)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        model.addAttribute("monthLabels", months.stream()
                .map(month -> month.format(DateTimeFormatter.ofPattern("MMM yy"))).toList());
        model.addAttribute("monthCounts", months.stream().map(month -> totals.getOrDefault(month, 0L)).toList());
    }

    private void addYearSeries(Model model, List<LostFoundReport> reports, List<Integer> years) {
        Map<Integer, Long> totals = reports.stream()
                .map(report -> report.getCreatedAt().getYear())
                .filter(years::contains)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        model.addAttribute("yearLabels", years);
        model.addAttribute("yearCounts", years.stream().map(year -> totals.getOrDefault(year, 0L)).toList());
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
