package com.regionalai.floatingball.server.modules.adminui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminUiController {

    @GetMapping("/")
    public String root() {
        return "redirect:/admin/";
    }

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/";
    }

    @GetMapping("/admin/")
    public String adminIndex() {
        return "forward:/admin/index.html";
    }
}
