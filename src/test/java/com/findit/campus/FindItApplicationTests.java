package com.findit.campus;

import com.findit.campus.repository.LocationRepository;
import com.findit.campus.repository.ClaimRepository;
import com.findit.campus.repository.UserRepository;
import com.findit.campus.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

        mockMvc.perform(get("/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")));

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
