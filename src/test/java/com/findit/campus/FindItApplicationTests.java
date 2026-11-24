package com.findit.campus;

import com.findit.campus.repository.LocationRepository;
import com.findit.campus.repository.ClaimRepository;
import com.findit.campus.repository.UserRepository;
import com.findit.campus.repository.NotificationRepository;
import com.findit.campus.repository.ReportRepository;
import com.findit.campus.repository.ReportCommentRepository;
import com.findit.campus.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
@AutoConfigureMockMvc
class FindItApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCommentRepository reportCommentRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void homePageRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Barangmu punya")));
    }


    @Test
    void reportSearchPageRenders() throws Exception {
        mockMvc.perform(get("/reports").param("q", "dompet"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Jelajahi")));
        mockMvc.perform(get("/reports").param("type", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"));
        mockMvc.perform(get("/reports").param("type", "FOUND"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"));
        mockMvc.perform(get("/reports").param("q", "dompet").param("type", "FOUND"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"));
    }

    @Test
    void creatingReportRequiresLogin() throws Exception {
        mockMvc.perform(get("/reports/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void userCanRegisterAndPublishAFoundItem() throws Exception {
        String email = "findit-test@example.edu";
        String password = "safe-password-123";
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "FindIt Test")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        var loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        Long locationId = locationRepository.findAllByOrderByNameAsc().getFirst().getId();

        var createResult = mockMvc.perform(post("/reports")
                        .session(session)
                        .with(csrf())
                        .param("title", "Tumbler uji")
                        .param("description", "Tumbler ditemukan dekat perpustakaan.")
                        .param("type", "FOUND")
                        .param("category", "OTHER")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-23T12:30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(get(createResult.getResponse().getRedirectedUrl()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("report-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tumbler uji")));
    }

    @Test
    void ownerCanApproveAClaimAndMarkItemReturned() throws Exception {
        String password = "safe-password-123";
        MockHttpSession ownerSession = registerAndLogin("owner-findit@example.edu", "Owner", password);
        Long locationId = locationRepository.findAllByOrderByNameAsc().getFirst().getId();
        var reportResult = mockMvc.perform(post("/reports")
                        .session(ownerSession)
                        .with(csrf())
                        .param("title", "Kartu akses uji")
                        .param("description", "Kartu akses ditemukan.")
                        .param("type", "FOUND")
                        .param("category", "ID_CARDS")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-23T12:30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Long reportId = Long.valueOf(reportResult.getResponse().getRedirectedUrl().substring(
                reportResult.getResponse().getRedirectedUrl().lastIndexOf('/') + 1));

        MockHttpSession claimantSession = registerAndLogin("claimant-findit@example.edu", "Claimant", password);
        mockMvc.perform(post("/reports/{id}/claims", reportId)
                        .session(claimantSession)
                        .with(csrf())
                        .param("message", "Ada stiker kecil di bagian belakang kartu."))
                .andExpect(status().is3xxRedirection());

        Long claimId = claimRepository.findByReportIdOrderByCreatedAtDesc(reportId).getFirst().getId();
        mockMvc.perform(post("/claims/{id}/decision", claimId)
                        .session(ownerSession)
                        .with(csrf())
                        .param("decision", "ACCEPTED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/claims"));

        mockMvc.perform(post("/reports/{id}/returned", reportId)
                        .session(ownerSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/reports/{id}", reportId).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dikembalikan")));
    }

    @Test
    void adminCanManageUsersAndModerateReports() throws Exception {
        String password = "safe-password-123";
        registerAndLogin("admin-findit@example.edu", "FindIt Admin", password);
        var adminUser = userRepository.findByEmail("admin-findit@example.edu").orElseThrow();
        adminUser.setRole(UserRole.ADMIN);
        userRepository.saveAndFlush(adminUser);
        MockHttpSession adminSession = login("admin-findit@example.edu", password);

        MockHttpSession aliasAdminSession = login("admin", password);
        mockMvc.perform(get("/admin").session(aliasAdminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
        mockMvc.perform(get("/").session(aliasAdminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(">Admin</a>"))));

        mockMvc.perform(get("/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tren laporan")));

        mockMvc.perform(post("/admin/users")
                        .session(adminSession)
                        .with(csrf())
                        .param("fullName", "Managed User")
                        .param("email", "managed-user@example.edu")
                        .param("nim", "J040300099")
                        .param("studyProgram", "Sistem Informasi")
                        .param("password", "safe-password-456")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection());
        var managedUser = userRepository.findByEmail("managed-user@example.edu").orElseThrow();
        mockMvc.perform(get("/admin/users/{id}/edit", managedUser.getId()).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-user-form"));
        mockMvc.perform(post("/admin/users/{id}", managedUser.getId())
                        .session(adminSession)
                        .with(csrf())
                        .param("fullName", "Updated Managed User")
                        .param("email", "managed-user@example.edu")
                        .param("nim", "J040300099")
                        .param("studyProgram", "Informatika")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals("Informatika",
                userRepository.findById(managedUser.getId()).orElseThrow().getStudyProgram());
        mockMvc.perform(post("/admin/users/{id}/delete", managedUser.getId())
                        .session(adminSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(managedUser.getId()));

        MockHttpSession userSession = registerAndLogin("regular-findit@example.edu", "Regular User", password);
        mockMvc.perform(get("/admin").session(userSession))
                .andExpect(status().isForbidden());

        var regularUser = userRepository.findByEmail("regular-findit@example.edu").orElseThrow();
        mockMvc.perform(post("/admin/users/{id}/role", regularUser.getId())
                        .session(adminSession)
                        .with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(UserRole.ADMIN,
                userRepository.findById(regularUser.getId()).orElseThrow().getRole());
        mockMvc.perform(get("/claims").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        mockMvc.perform(post("/admin/users/{id}/role", adminUser.getId())
                        .session(adminSession)
                        .with(csrf())
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(UserRole.ADMIN,
                userRepository.findById(adminUser.getId()).orElseThrow().getRole());

        Long locationId = locationRepository.findAllByOrderByNameAsc().getFirst().getId();
        var reportResult = mockMvc.perform(post("/reports")
                        .session(adminSession)
                        .with(csrf())
                        .param("title", "Laporan untuk moderasi")
                        .param("description", "Laporan pengujian admin.")
                        .param("type", "LOST")
                        .param("category", "OTHER")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-23T12:30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Long reportId = Long.valueOf(reportResult.getResponse().getRedirectedUrl().substring(
                reportResult.getResponse().getRedirectedUrl().lastIndexOf('/') + 1));

        mockMvc.perform(post("/admin/reports/{id}/delete", reportId)
                        .session(adminSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/reports/{id}", reportId))
                .andExpect(status().isNotFound());
    }

    @Test
    void profileMatchNotificationsAndReportCommentsWork() throws Exception {
        String password = "safe-password-123";
        MockHttpSession ownerSession = registerAndLogin("lost-owner@example.edu", "Lost Owner", password);
        mockMvc.perform(post("/profile")
                        .session(ownerSession)
                        .with(csrf())
                        .param("fullName", "Pemilik Dompet")
                        .param("nim", "J040300001")
                        .param("studyProgram", "Informatika"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
        mockMvc.perform(get("/profile").session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("J040300001")));

        var lostOwner = userRepository.findByEmail("lost-owner@example.edu").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("J040300001", lostOwner.getNim());
        org.junit.jupiter.api.Assertions.assertEquals("Informatika", lostOwner.getStudyProgram());
        Long locationId = locationRepository.findAllByOrderByNameAsc().getFirst().getId();
        mockMvc.perform(post("/reports")
                        .session(ownerSession)
                        .with(csrf())
                        .param("title", "Dompet kulit cokelat")
                        .param("description", "Dompet hilang di area kampus.")
                        .param("type", "LOST")
                        .param("category", "OTHER")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-23T12:30"))
                .andExpect(status().is3xxRedirection());

        MockHttpSession finderSession = registerAndLogin("finder@example.edu", "Penemu Barang", password);
        var foundResult = mockMvc.perform(post("/reports")
                        .session(finderSession)
                        .with(csrf())
                        .param("title", "Dompet kulit ditemukan")
                        .param("description", "Ditemukan di kantin.")
                        .param("type", "FOUND")
                        .param("category", "OTHER")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-24T12:30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Long foundReportId = reportIdFromRedirect(foundResult.getResponse().getRedirectedUrl());
        org.junit.jupiter.api.Assertions.assertEquals(1,
                notificationRepository.countByRecipientIdAndReadFalse(lostOwner.getId()));

        mockMvc.perform(post("/reports/{id}/comments", foundReportId)
                        .session(ownerSession)
                        .with(csrf())
                        .param("message", "Apakah ada inisial pada bagian dalam?"))
                .andExpect(status().is3xxRedirection());
        var image = new MockMultipartFile("image", "proof.png", "image/png", onePixelPng());
        mockMvc.perform(multipart("/reports/{id}/comments", foundReportId)
                        .file(image)
                        .session(finderSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        var imageComment = reportCommentRepository.findByReportIdOrderByCreatedAtAsc(foundReportId).getLast();
        Path savedImage = Path.of("target/test-uploads")
                .resolve(imageComment.getImagePath().substring("/uploads/".length()));
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(savedImage));
        mockMvc.perform(post("/comments/{id}/delete", imageComment.getId())
                        .session(finderSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(savedImage));

        mockMvc.perform(get("/reports/{id}", foundReportId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Apakah ada inisial")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pemilik Dompet")));
        mockMvc.perform(get("/notifications").session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dompet kulit ditemukan")));
    }

    @Test
    void reportOwnerCanEditAndDeleteTheirReport() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("report-editor@example.edu", "Report Editor",
                "safe-password-123");
        Long locationId = locationRepository.findAllByOrderByNameAsc().getFirst().getId();
        var reportResult = mockMvc.perform(post("/reports")
                        .session(ownerSession)
                        .with(csrf())
                        .param("title", "Nama awal")
                        .param("description", "Deskripsi awal.")
                        .param("type", "LOST")
                        .param("category", "OTHER")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-23T12:30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Long reportId = reportIdFromRedirect(reportResult.getResponse().getRedirectedUrl());

        mockMvc.perform(get("/reports/{id}/edit", reportId).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("report-form"));
        mockMvc.perform(post("/reports/{id}/edit", reportId)
                        .session(ownerSession)
                        .with(csrf())
                        .param("title", "Nama diperbarui")
                        .param("description", "Deskripsi diperbarui.")
                        .param("type", "LOST")
                        .param("category", "OTHER")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-23T12:30"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals("Nama diperbarui",
                reportRepository.findById(reportId).orElseThrow().getTitle());

        mockMvc.perform(post("/reports/{id}/delete", reportId)
                        .session(ownerSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(reportRepository.existsById(reportId));
    }

    @Test
    void userCanUploadReportImageAndImageIsRemovedWithReport() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("image-owner@example.edu", "Image Owner",
                "safe-password-123");
        Long locationId = locationRepository.findAllByOrderByNameAsc().getFirst().getId();
        var image = new MockMultipartFile("image", "item.png", "image/png", onePixelPng());
        var createResult = mockMvc.perform(multipart("/reports")
                        .file(image)
                        .session(ownerSession)
                        .with(csrf())
                        .param("title", "Tas bergambar")
                        .param("description", "Tas ditemukan di kampus.")
                        .param("type", "FOUND")
                        .param("category", "BAGS")
                        .param("locationId", locationId.toString())
                        .param("occurredAt", "2026-11-24T12:30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Long reportId = reportIdFromRedirect(createResult.getResponse().getRedirectedUrl());
        var report = reportRepository.findById(reportId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(report.getImagePath());
        Path savedImage = Path.of("target/test-uploads")
                .resolve(report.getImagePath().substring("/uploads/".length()));
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(savedImage));
        mockMvc.perform(get("/reports/{id}", reportId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(report.getImagePath())));

        mockMvc.perform(post("/reports/{id}/delete", reportId)
                        .session(ownerSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(savedImage));
    }

    private Long reportIdFromRedirect(String redirectUrl) {
        return Long.valueOf(redirectUrl.substring(redirectUrl.lastIndexOf('/') + 1));
    }

    private byte[] onePixelPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private MockHttpSession login(String email, String password) throws Exception {
        var loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private MockHttpSession registerAndLogin(String email, String name, String password) throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", name)
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection());
        var loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
