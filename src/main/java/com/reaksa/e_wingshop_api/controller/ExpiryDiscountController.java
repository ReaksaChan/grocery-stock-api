package com.reaksa.e_wingshop_api.controller;

import com.reaksa.e_wingshop_api.dto.request.ExpiryDiscountRequest;
import com.reaksa.e_wingshop_api.dto.response.ExpiryDiscountResponse;
import com.reaksa.e_wingshop_api.dto.response.PageResponse;
import com.reaksa.e_wingshop_api.enums.DiscountTier;
import com.reaksa.e_wingshop_api.repository.UserRepository;
import com.reaksa.e_wingshop_api.service.ExpiryDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN','MANAGER')")
public class ExpiryDiscountController {

    private final ExpiryDiscountService discountService;
    private final UserRepository userRepository;

    // ── GET /api/v1/discounts/active ──────────────────────────────────
    /** All currently active discounts, optionally filtered by branch. */
    @GetMapping("/active")
    public ResponseEntity<PageResponse<ExpiryDiscountResponse>> getActive(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ExpiryDiscountResponse> result = discountService
                .getActiveDiscountsPaged(branchId, page, size)
                .map(ExpiryDiscountResponse::from);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    // ── GET /api/v1/discounts/{id} ────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ExpiryDiscountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ExpiryDiscountResponse.from(discountService.findById(id)));
    }

    // ── GET /api/v1/discounts/inventory/{inventoryId} ─────────────────
    /** Full discount history for one inventory record. */
    @GetMapping("/inventory/{inventoryId}")
    public ResponseEntity<List<ExpiryDiscountResponse>> getByInventory(
            @PathVariable Long inventoryId) {
        return ResponseEntity.ok(
                discountService.getHistoryForInventory(inventoryId).stream()
                        .map(ExpiryDiscountResponse::from)
                        .toList());
    }

    // ── GET /api/v1/discounts/inventory/{inventoryId}/active ──────────
    /** Check if a specific inventory has an active discount right now. */
    @GetMapping("/inventory/{inventoryId}/active")
    public ResponseEntity<ExpiryDiscountResponse> getActiveForInventory(
            @PathVariable Long inventoryId) {
        return discountService.getActiveForInventory(inventoryId)
                .map(d -> ResponseEntity.ok(ExpiryDiscountResponse.from(d)))
                .orElse(ResponseEntity.noContent().build());
    }

    // ── POST /api/v1/discounts ────────────────────────────────────────
    /** Create a single expiry discount on one inventory record. */
    @PostMapping
    public ResponseEntity<ExpiryDiscountResponse> create(
            @Valid @RequestBody ExpiryDiscountRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        var actor = resolveActor(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ExpiryDiscountResponse.from(
                        discountService.createDiscount(request, actor.id(), actor.email())));
    }

    // ── POST /api/v1/discounts/auto-apply ─────────────────────────────
    /**
     * Auto-apply discounts to ALL inventory records expiring within a tier's
     * window that don't already have an active discount.
     *
     * Body: { "tier": "TWO_WEEKS", "branchId": 1 }  (branchId optional)
     */
    @PostMapping("/auto-apply")
    public ResponseEntity<Map<String, Object>> autoApply(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        DiscountTier tier = DiscountTier.valueOf(
                body.getOrDefault("tier", "").toString().toUpperCase());
        Long branchId = body.containsKey("branchId")
                ? Long.valueOf(body.get("branchId").toString()) : null;

        var actor = resolveActor(principal);
        int created = discountService.autoApplyByTier(tier, branchId, actor.id(), actor.email());

        return ResponseEntity.ok(Map.of(
                "tier",       tier.name(),
                "tierLabel",  tier.getLabel(),
                "branchId",   branchId != null ? branchId : "all",
                "created",    created,
                "message",    created + " discount(s) created for tier: " + tier.getLabel()
        ));
    }

    // ── DELETE /api/v1/discounts/{id}/revoke ──────────────────────────
    @DeleteMapping("/{id}/revoke")
    public ResponseEntity<ExpiryDiscountResponse> revoke(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Manually revoked") String reason,
            @AuthenticationPrincipal UserDetails principal) {

        var actor = resolveActor(principal);
        return ResponseEntity.ok(ExpiryDiscountResponse.from(
                discountService.revoke(id, reason, actor.id(), actor.email())));
    }

    // ── GET /api/v1/discounts/tiers ───────────────────────────────────
    /** Returns all available tiers with their default rates — for UI dropdowns. */
    @GetMapping("/tiers")
    public ResponseEntity<List<Map<String, Object>>> getTiers() {
        List<Map<String, Object>> tiers = Arrays.stream(DiscountTier.values())
                .map(t -> Map.<String, Object>of(
                        "tier",            t.name(),
                        "label",           t.getLabel(),
                        "daysWindow",      t.getDaysWindow(),
                        "defaultRatePct",  t.getDefaultRatePct()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(tiers);
    }

    // ── Helper ────────────────────────────────────────────────────────
    private record Actor(Long id, String email) {}

    private Actor resolveActor(UserDetails principal) {
        var user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return new Actor(user.getId(), user.getEmail());
    }
}
