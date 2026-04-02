package com.reaksa.e_wingshop_api.controller;

import com.reaksa.e_wingshop_api.entity.User;
import com.reaksa.e_wingshop_api.enums.RoleName;
import com.reaksa.e_wingshop_api.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Admin — list all users ────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','SUPERADMIN')")
    public ResponseEntity<Page<User>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.findAll(page, size));
    }

    // ── Admin — get any user by id ────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','SUPERADMIN')")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // ── Owner — create staff / admin account ──────────────────────────
    @PostMapping("/staff")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<User> createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                userService.createStaff(req.fullName, req.email,
                        req.password, req.phone, req.role, req.branchId));
    }

    // ── Owner — change role ───────────────────────────────────────────
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<User> changeRole(@PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        RoleName role = RoleName.valueOf(body.get("role").toUpperCase());
        return ResponseEntity.ok(userService.changeRole(id, role));
    }

    // ── Owner — assign a user as branch manager ──────────────────────
    @PatchMapping("/{id}/manager-branch")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<User> assignManagerBranch(@PathVariable Long id,
                                                    @Valid @RequestBody AssignManagerBranchRequest req) {
        return ResponseEntity.ok(userService.assignManagerToBranch(id, req.branchId));
    }

    // ── Owner/Admin — reset password ──────────────────────────────────
    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return ResponseEntity.noContent().build();
    }

    // ── Authenticated user — view own profile ─────────────────────────
    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.findByEmail(principal.getUsername()));
    }

    // ── Authenticated user — update own profile ───────────────────────
    @PatchMapping("/me")
    public ResponseEntity<User> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {
        User me = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(
                userService.updateProfile(me.getId(), body.get("fullName"), body.get("phone")));
    }

    // ── Inner DTO ─────────────────────────────────────────────────────
    @Data
    public static class CreateStaffRequest {
        @NotBlank @Size(max = 100) String fullName;
        @NotBlank @Email           String email;
        @NotBlank @Size(min = 8)   String password;
        String phone;
        @NotNull                   RoleName role;
        Long branchId;
    }

    @Data
    public static class AssignManagerBranchRequest {
        @NotNull @Positive Long branchId;
    }
}
