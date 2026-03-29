package com.reaksa.e_wingshop_api.service;

import com.reaksa.e_wingshop_api.dto.request.ProductRequest;
import com.reaksa.e_wingshop_api.entity.Category;
import com.reaksa.e_wingshop_api.entity.Product;
import com.reaksa.e_wingshop_api.exception.DuplicateResourceException;
import com.reaksa.e_wingshop_api.exception.ResourceNotFoundException;
import com.reaksa.e_wingshop_api.repository.CategoryRepository;
import com.reaksa.e_wingshop_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<Product> search(Long categoryId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.search(categoryId, keyword, pageable);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public Product findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
    }

    @Transactional
    public Product create(ProductRequest request) {
        if (request.getBarcode() != null
                && productRepository.findByBarcode(request.getBarcode()).isPresent()) {
            throw new DuplicateResourceException("Barcode already exists: " + request.getBarcode());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .barcode(request.getBarcode())
                .imageUrl(request.getImageUrl())
                .category(category)
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);

        if (request.getBarcode() != null && !request.getBarcode().equals(product.getBarcode())) {
            productRepository.findByBarcode(request.getBarcode()).ifPresent(p -> {
                throw new DuplicateResourceException("Barcode already in use: " + request.getBarcode());
            });
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBarcode(request.getBarcode());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        product.setIsActive(false);          // soft delete
        productRepository.save(product);
    }
}
