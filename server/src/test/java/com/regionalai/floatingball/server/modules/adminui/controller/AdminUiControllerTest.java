package com.regionalai.floatingball.server.modules.adminui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminUiControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUiController()).build();
    }

    @Test
    void rootShouldRedirectToAdminEntry() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/"));
    }

    @Test
    void adminWithoutSlashShouldRedirectToAdminEntry() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/"));
    }

    @Test
    void adminWithSlashShouldForwardToStaticIndex() throws Exception {
        mockMvc.perform(get("/admin/"))
            .andExpect(status().isOk())
            .andExpect(view().name("forward:/admin/index.html"));
    }
}
