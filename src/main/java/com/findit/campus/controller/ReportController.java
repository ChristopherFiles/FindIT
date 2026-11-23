package com.findit.campus.controller;

import com.findit.campus.dto.ClaimForm;
import com.findit.campus.dto.ReportForm;
import com.findit.campus.entity.AppUser;
import com.findit.campus.entity.ClaimStatus;
import com.findit.campus.entity.ItemCategory;
import com.findit.campus.entity.LostFoundReport;
import com.findit.campus.entity.ReportType;
import com.findit.campus.service.FindItService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
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
                    reportForm.getOccurredAt(), user.getEmail());
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
        model.addAttribute("claimForm", new ClaimForm());
        boolean isOwner = user != null && report.getOwner().getId().equals(user.getId());
        model.addAttribute("isOwner", isOwner);
        if (isOwner) {
            model.addAttribute("claims", findItService.claimsForReport(id));
        }
        return "report-detail";
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
}
