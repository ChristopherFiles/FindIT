package com.findit.campus.controller;

import com.findit.campus.dto.ClaimForm;
import com.findit.campus.dto.CommentForm;
import com.findit.campus.dto.ReportForm;
import com.findit.campus.entity.AppUser;
import com.findit.campus.entity.ClaimStatus;
import com.findit.campus.entity.ItemCategory;
import com.findit.campus.entity.LostFoundReport;
import com.findit.campus.entity.ReportType;
import com.findit.campus.entity.UserRole;
import com.findit.campus.service.FindItService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class ReportController {
    private final FindItService findItService;

    public ReportController(FindItService findItService) {
        this.findItService = findItService;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) String q,
                          @RequestParam(required = false) ReportType type, Model model) {
        model.addAttribute("reports", findItService.search(q, type));
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("selectedType", type);
        model.addAttribute("reportTypes", ReportType.values());
        return "reports";
    }

    @GetMapping({"/reports/new", "/laporan/buat"})
    public String newReport(Model model) {
        ReportForm form = new ReportForm();
        form.setOccurredAt(LocalDateTime.now().withSecond(0).withNano(0));
        model.addAttribute("reportForm", form);
        model.addAttribute("editMode", false);
        addReportOptions(model);
        return "report-form";
    }

    @PostMapping({"/reports", "/laporan"})
    public String createReport(@Valid @ModelAttribute ReportForm reportForm,
                               BindingResult bindingResult, Model model,
                               @AuthenticationPrincipal AppUser user,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addReportOptions(model);
            return "report-form";
        }
        try {
            var report = findItService.createReport(reportForm.getTitle(), reportForm.getDescription(),
                    reportForm.getType(), reportForm.getCategory(), reportForm.getLocationId(),
                    reportForm.getOccurredAt(), user.getEmail(), reportForm.getImage());
            redirectAttributes.addFlashAttribute("successMessage", "Laporan berhasil dipublikasikan.");
            return "redirect:/reports/" + report.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            addReportOptions(model);
            return "report-form";
        }
    }

    @GetMapping({"/reports/{id}", "/laporan/{id}"})
    public String reportDetails(@PathVariable Long id, Model model,
                                @AuthenticationPrincipal AppUser user) {
        LostFoundReport report;
        try {
            report = findItService.report(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
        model.addAttribute("report", report);
        model.addAttribute("currentImagePath", report.getImagePath());
        model.addAttribute("claimForm", new ClaimForm());
        model.addAttribute("commentForm", new CommentForm());
        model.addAttribute("comments", findItService.commentsForReport(id));
        boolean isOwner = user != null && report.getOwner().getId().equals(user.getId());
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canManage", user != null
                && (isOwner || user.getRole() == UserRole.ADMIN));
        if (isOwner) {
            model.addAttribute("claims", findItService.claimsForReport(id));
        }
        return "report-detail";
    }

    @GetMapping("/reports/{id}/edit")
    public String editReport(@PathVariable Long id, @AuthenticationPrincipal AppUser user, Model model) {
        LostFoundReport report = findItService.report(id);
        if (user.getRole() == UserRole.ADMIN || report.getOwner().getId().equals(user.getId())) {
            ReportForm form = new ReportForm();
            form.setTitle(report.getTitle());
            form.setDescription(report.getDescription());
            form.setType(report.getType());
            form.setCategory(report.getCategory());
            form.setLocationId(report.getLocation().getId());
            form.setOccurredAt(report.getOccurredAt());
            model.addAttribute("currentImagePath", report.getImagePath());
            model.addAttribute("reportForm", form);
            model.addAttribute("reportId", id);
            model.addAttribute("editMode", true);
            addReportOptions(model);
            return "report-form";
        }
        throw new AccessDeniedException("Hanya pemilik laporan atau admin yang dapat mengubah laporan ini.");
    }

    @PostMapping("/reports/{id}/edit")
    public String updateReport(@PathVariable Long id, @Valid @ModelAttribute ReportForm reportForm,
                               BindingResult bindingResult, @AuthenticationPrincipal AppUser user,
                               Model model, RedirectAttributes redirectAttributes) {
        findItService.ensureReportManager(id, user);
        if (bindingResult.hasErrors()) {
            model.addAttribute("reportId", id);
            model.addAttribute("editMode", true);
            addReportOptions(model);
            return "report-form";
        }
        try {
            findItService.updateReport(id, reportForm, user);
            redirectAttributes.addFlashAttribute("successMessage", "Laporan berhasil diperbarui.");
            return "redirect:/reports/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            model.addAttribute("reportId", id);
            model.addAttribute("editMode", true);
            addReportOptions(model);
            return "report-form";
        }
    }

    @PostMapping("/reports/{id}/delete")
    public String deleteReport(@PathVariable Long id, @AuthenticationPrincipal AppUser user,
                               RedirectAttributes redirectAttributes) {
        try {
            findItService.deleteReport(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Laporan berhasil dihapus.");
            return "redirect:/my-reports";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/reports";
        }
    }

    @PostMapping("/reports/{id}/comments")
    public String addComment(@PathVariable Long id, @Valid @ModelAttribute CommentForm commentForm,
                             BindingResult bindingResult,
                             @RequestParam(required = false) MultipartFile image,
                             @AuthenticationPrincipal AppUser user,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Komentar maksimal 1.200 karakter.");
            return "redirect:/reports/" + id + "#komentar";
        }
        try {
            findItService.addComment(id, commentForm.getMessage(), image, user);
            redirectAttributes.addFlashAttribute("successMessage", "Komentar berhasil ditambahkan.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/reports/" + id + "#komentar";
    }

    @PostMapping("/comments/{id}/edit")
    public String editComment(@PathVariable Long id, @Valid @ModelAttribute CommentForm commentForm,
                              BindingResult bindingResult,
                              @RequestParam(required = false) MultipartFile image,
                              @AuthenticationPrincipal AppUser user,
                              RedirectAttributes redirectAttributes) {
        Long reportId = commentReportId(id);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Komentar maksimal 1.200 karakter.");
            return "redirect:/reports/" + reportId + "#komentar";
        }
        try {
            findItService.updateComment(id, commentForm.getMessage(), image, user);
            redirectAttributes.addFlashAttribute("successMessage", "Komentar berhasil diperbarui.");
            return "redirect:/reports/" + reportId + "#komentar";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/reports/" + reportId + "#komentar";
        }
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @AuthenticationPrincipal AppUser user,
                                RedirectAttributes redirectAttributes) {
        Long reportId = commentReportId(id);
        try {
            findItService.deleteComment(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Komentar berhasil dihapus.");
            return "redirect:/reports/" + reportId + "#komentar";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/reports/" + reportId + "#komentar";
        }
    }

    @PostMapping({"/reports/{id}/claims", "/laporan/{id}/klaim"})
    public String createClaim(@PathVariable Long id, @Valid @ModelAttribute ClaimForm claimForm,
                              BindingResult bindingResult, @AuthenticationPrincipal AppUser user,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Pesan klaim wajib diisi (maksimal 1.200 karakter).");
            return "redirect:/reports/" + id;
        }
        try {
            findItService.createClaim(id, claimForm.getMessage(), user.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Klaim terkirim. Pantau statusnya di halaman klaim.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/reports/" + id;
    }

    @PostMapping("/reports/{id}/returned")
    public String markReturned(@PathVariable Long id, @AuthenticationPrincipal AppUser user,
                               RedirectAttributes redirectAttributes) {
        try {
            findItService.markReturned(id, user.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Barang ditandai sudah dikembalikan.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/reports/" + id;
    }

    @GetMapping("/my-reports")
    public String myReports(@AuthenticationPrincipal AppUser user, Model model) {
        model.addAttribute("reports", findItService.myReports(user.getId()));
        return "my-reports";
    }

    @GetMapping("/claims")
    public String claims(@AuthenticationPrincipal AppUser user, Model model) {
        model.addAttribute("myClaims", findItService.myClaims(user.getId()));
        model.addAttribute("incomingClaims", findItService.claimsForOwner(user.getId()));
        model.addAttribute("claimStatuses", ClaimStatus.values());
        return "claims";
    }

    @PostMapping("/claims/{id}/decision")
    public String decideClaim(@PathVariable Long id, @RequestParam ClaimStatus decision,
                              @AuthenticationPrincipal AppUser user,
                              RedirectAttributes redirectAttributes) {
        try {
            findItService.decideClaim(id, decision, user.getEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    decision == ClaimStatus.ACCEPTED ? "Klaim disetujui." : "Klaim ditolak.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/claims";
    }

    private void addReportOptions(Model model) {
        model.addAttribute("locations", findItService.locations());
        model.addAttribute("reportTypes", ReportType.values());
        model.addAttribute("categories", ItemCategory.values());
    }

    private Long commentReportId(Long id) {
        try {
            return findItService.commentReportId(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
