package com.skyline.org.auth.controller;

import com.skyline.org.auth.dto.AdminUserView;
import com.skyline.org.auth.service.AdminUserService;
import com.skyline.org.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<AdminUserView> users = adminUserService.listUsers(PageRequest.of(page, 20));
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{id}/enable")
    public String enableUser(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "username") String actor,
            RedirectAttributes redirectAttributes) {
        return runAdminAction(redirectAttributes, "User enabled", () -> adminUserService.setEnabled(id, true, actor));
    }

    @PostMapping("/users/{id}/disable")
    public String disableUser(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "username") String actor,
            RedirectAttributes redirectAttributes) {
        return runAdminAction(redirectAttributes, "User disabled", () -> adminUserService.setEnabled(id, false, actor));
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "username") String actor,
            RedirectAttributes redirectAttributes) {
        return runAdminAction(redirectAttributes, "User unlocked", () -> adminUserService.unlockUser(id, actor));
    }

    @PostMapping("/users/{id}/grant-admin")
    public String grantAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "username") String actor,
            RedirectAttributes redirectAttributes) {
        return runAdminAction(redirectAttributes, "Admin role granted", () -> adminUserService.grantAdminRole(id, actor));
    }

    @PostMapping("/users/{id}/revoke-admin")
    public String revokeAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "username") String actor,
            RedirectAttributes redirectAttributes) {
        return runAdminAction(redirectAttributes, "Admin role revoked", () -> adminUserService.revokeAdminRole(id, actor));
    }

    private String runAdminAction(RedirectAttributes redirectAttributes, String successMessage, Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
