package com.reaksa.e_wingshop_api.service;

import com.reaksa.e_wingshop_api.dto.response.InventoryResponse;
import com.reaksa.e_wingshop_api.dto.request.InventoryRequest;
import com.reaksa.e_wingshop_api.entity.Branch;
import com.reaksa.e_wingshop_api.entity.Inventory;
import com.reaksa.e_wingshop_api.entity.Product;
import com.reaksa.e_wingshop_api.exception.InsufficientStockException;
import com.reaksa.e_wingshop_api.exception.ResourceNotFoundException;
import com.reaksa.e_wingshop_api.repository.BranchRepository;
import com.reaksa.e_wingshop_api.repository.InventoryRepository;
import com.reaksa.e_wingshop_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    // ── Read ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InventoryResponse> findAllStock(Long branchId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return inventoryRepository.findAllStock(branchId, pageable)
                .map(InventoryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<Inventory> findByBranch(Long branchId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return inventoryRepository.findByBranchId(branchId, pageable);
    }

    @Transactional(readOnly = true)
    public Inventory findByBranchAndProduct(Long branchId, Long productId) {
        return inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for branch %d / product %d".formatted(branchId, productId)));
    }

    @Transactional(readOnly = true)
    public List<Inventory> getLowStock(Long branchId) {
        return inventoryRepository.findLowStock(branchId);
    }

    @Transactional(readOnly = true)
    public List<Inventory> getExpiringSoon(Long branchId, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(daysAhead);
        return inventoryRepository.findExpiringSoon(today, cutoff, branchId);
    }

    @Transactional(readOnly = true)
    public List<Inventory> getExpired() {
        return inventoryRepository.findExpired(LocalDate.now());
    }

    // ── Write ─────────────────────────────────────────────────────────

    @Transactional
    public Inventory upsert(InventoryRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        Inventory inv = inventoryRepository
                .findByBranchIdAndProductId(request.getBranchId(), request.getProductId())
                .orElse(Inventory.builder().branch(branch).product(product).build());

        inv.setQuantity(request.getQuantity());
        inv.setLowStockThreshold(request.getLowStockThreshold());
        inv.setExpiryDate(request.getExpiryDate());

        return inventoryRepository.save(inv);
    }

    /**
     * Adjust quantity by delta (positive = restock, negative = manual removal).
     */
    @Transactional
    public Inventory adjust(Long branchId, Long productId, int delta, String reason) {
        Inventory inv = findByBranchAndProduct(branchId, productId);
        int newQty = inv.getQuantity() + delta;
        if (newQty < 0) {
            throw new InsufficientStockException(inv.getProduct().getName(),
                    Math.abs(delta), inv.getQuantity());
        }
        inv.setQuantity(newQty);
        Inventory saved = inventoryRepository.save(inv);
        log.info("Inventory adjusted — branch={} product={} delta={} reason={} newQty={}",
                branchId, productId, delta, reason, newQty);
        return saved;
    }

    /**
     * Transfer stock from one branch to another atomically.
     */
    @Transactional
    public void transfer(Long fromBranchId, Long toBranchId, Long productId, int quantity) {
        Inventory from = findByBranchAndProduct(fromBranchId, productId);
        if (from.getQuantity() < quantity) {
            throw new InsufficientStockException(from.getProduct().getName(),
                    quantity, from.getQuantity());
        }

        from.setQuantity(from.getQuantity() - quantity);
        inventoryRepository.save(from);

        Inventory to = inventoryRepository
                .findByBranchIdAndProductId(toBranchId, productId)
                .orElseGet(() -> {
                    Branch branch = branchRepository.findById(toBranchId)
                            .orElseThrow(() -> new ResourceNotFoundException("Branch", toBranchId));
                    return Inventory.builder()
                            .branch(branch)
                            .product(from.getProduct())
                            .quantity(0)
                            .lowStockThreshold(from.getLowStockThreshold())
                            .build();
                });

        to.setQuantity(to.getQuantity() + quantity);
        inventoryRepository.save(to);

        log.info("Stock transfer — product={} qty={} from={} to={}",
                productId, quantity, fromBranchId, toBranchId);
    }

    /**
     * Decrement stock when an order is fulfilled — throws if any item is short.
     */
//    @Transactional
//    public void fulfillOrder(Long branchId, List<OrderItem> items) {
//        for (OrderItem item : items) {
//            Inventory inv = findByBranchAndProduct(branchId, item.getProduct().getId());
//            if (inv.getQuantity() < item.getQuantity()) {
//                throw new InsufficientStockException(
//                        item.getProduct().getName(), item.getQuantity(), inv.getQuantity());
//            }
//            inv.setQuantity(inv.getQuantity() - item.getQuantity());
//            inventoryRepository.save(inv);
//        }
//    }
}
