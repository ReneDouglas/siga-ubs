package br.com.tecsus.sigaubs.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminSessionController {

    @GetMapping("/admin")
    public String getAdminHome() {
        return "redirect:/admin/tenant-management";
    }

    @GetMapping("/admin/login")
    public String getAdminLoginPage() {
        return "sessionManagement/admin_login";
    }

    @GetMapping("/admin/login-error")
    public String getAdminLoginErrorPage(Model model) {
        model.addAttribute("loginError", true);
        return "sessionManagement/admin_login";
    }
}
