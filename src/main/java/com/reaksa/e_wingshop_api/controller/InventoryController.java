package com.reaksa.e_wingshop_api.controller;

import com.reaksa.e_wingshop_api.dto.request.InventoryRequest;
import com.reaksa.e_wingshop_api.dto.response.InventoryResponse;
import com.reaksa.e_wingshop_api.dto.response.PageResponse;
import com.reaksa.e_wingshop_api.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN','MANAGER')")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<PageResponse<InventoryResponse>> listAllStock(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventoryResponse> result = inventoryService.findAllStock(branchId, page, size);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<PageResponse<InventoryResponse>> getByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<InventoryResponse> result = inventoryService.findByBranch(branchId, page, size)
                .map(InventoryResponse::from);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> upsert(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InventoryResponse.from(inventoryService.upsert(request)));
    }

    @PatchMapping("/adjust")
    public ResponseEntity<InventoryResponse> adjust(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam int delta,
            @RequestParam(defaultValue = "Manual adjustment") String reason) {
        return ResponseEntity.ok(
                InventoryResponse.from(inventoryService.adjust(branchId, productId, delta, reason)));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(
            @RequestParam Long fromBranchId,
            @RequestParam Long toBranchId,
            @RequestParam Long productId,
            @RequestParam @Min(1) int quantity) {
        inventoryService.transfer(fromBranchId, toBranchId, productId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> lowStock(
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(inventoryService.getLowStock(branchId).stream()
                .map(InventoryResponse::from).toList());
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<InventoryResponse>> expiringSoon(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(inventoryService.getExpiringSoon(branchId, daysAhead).stream()
                .map(InventoryResponse::from).toList());
    }

    @GetMapping("/expired")
    public ResponseEntity<List<InventoryResponse>> expired() {
        return ResponseEntity.ok(inventoryService.getExpired().stream()
                .map(InventoryResponse::from).toList());
    }
}
