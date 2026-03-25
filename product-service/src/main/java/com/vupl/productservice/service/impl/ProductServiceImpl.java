package com.vupl.productservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupl.productservice.dto.request.CategoryRequest;
import com.vupl.productservice.dto.request.ProductRequest;
import com.vupl.productservice.dto.request.ProductSearchRequest;
import com.vupl.productservice.dto.response.CategoryResponse;
import com.vupl.productservice.dto.response.PageResponse;
import com.vupl.productservice.dto.response.ProductResponse;
import com.vupl.productservice.entity.*;
import com.vupl.productservice.event.ProductEventPayload;
import com.vupl.productservice.exception.AppException;
import com.vupl.productservice.repository.CategoryRepository;
import com.vupl.productservice.repository.OutboxEventRepository;
import com.vupl.productservice.repository.ProductRepository;
import com.vupl.productservice.service.ProductService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.product-updated}") private String productUpdatedTopic;
    @Value("${kafka.topics.product-deleted}") private String productDeletedTopic;

    // ── Category ─────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNullAndIsActiveTrue()
                .stream().map(CategoryResponse::from).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(String parentId) {
        return categoryRepository.findByParentIdAndIsActiveTrue(parentId)
                .stream().map(CategoryResponse::from).collect(Collectors.toList());
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug()))
            throw AppException.conflict("Slug đã tồn tại: " + request.getSlug());
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> AppException.notFound("Danh mục cha không tồn tại"));
        }
        Category category = Category.builder()
                .name(request.getName()).slug(request.getSlug()).parent(parent)
                .sortOrder(request.getSortOrder()).isActive(request.getIsActive()).build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Danh mục không tồn tại"));
        if (!category.getSlug().equals(request.getSlug()) && categoryRepository.existsBySlug(request.getSlug()))
            throw AppException.conflict("Slug đã tồn tại: " + request.getSlug());
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setSortOrder(request.getSortOrder());
        category.setIsActive(request.getIsActive());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Danh mục không tồn tại"));
        categoryRepository.delete(category);
    }

    // ── Product ──────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(ProductSearchRequest req) {
        Product.ProductStatus status = null;
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try { status = Product.ProductStatus.valueOf(req.getStatus()); }
            catch (IllegalArgumentException ignored) {}
        }
        Sort sort = req.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(req.getSortBy()).ascending() : Sort.by(req.getSortBy()).descending();
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);
        Page<Product> page = productRepository.search(
                req.getKeyword(), req.getCategoryId(), status, req.getMinPrice(), req.getMaxPrice(), pageable);
        return PageResponse.from(page.map(ProductResponse::from));
    }

    @Override @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        return ProductResponse.from(findById(id));
    }

    @Override @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        return ProductResponse.from(productRepository.findBySlug(slug)
                .orElseThrow(() -> AppException.notFound("Sản phẩm không tồn tại")));
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku()))
            throw AppException.conflict("SKU đã tồn tại: " + request.getSku());
        if (productRepository.existsBySlug(request.getSlug()))
            throw AppException.conflict("Slug đã tồn tại: " + request.getSlug());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> AppException.notFound("Danh mục không tồn tại"));

        Product product = Product.builder()
                .sku(request.getSku()).name(request.getName()).slug(request.getSlug())
                .description(request.getDescription()).price(request.getPrice()).salePrice(request.getSalePrice())
                .category(category).brand(request.getBrand()).weightGram(request.getWeightGram())
                .status(Product.ProductStatus.valueOf(request.getStatus()))
                .build();

        // Images
        if (request.getImageUrls() != null) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                images.add(ProductImage.builder().product(product)
                        .url(request.getImageUrls().get(i)).sortOrder(i).isPrimary(i == 0).build());
            }
            product.setImages(images);
        }

        // Attributes
        if (request.getAttributes() != null) {
            List<ProductAttribute> attrs = request.getAttributes().entrySet().stream()
                    .map(e -> ProductAttribute.builder().product(product).attrKey(e.getKey()).attrValue(e.getValue()).build())
                    .collect(Collectors.toList());
            product.setAttributes(attrs);
        }

        Product saved = productRepository.save(product);
        saveOutboxEvent(saved, productUpdatedTopic);
        return ProductResponse.from(saved);
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = findById(id);

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku()))
            throw AppException.conflict("SKU đã tồn tại: " + request.getSku());
        if (!product.getSlug().equals(request.getSlug()) && productRepository.existsBySlug(request.getSlug()))
            throw AppException.conflict("Slug đã tồn tại: " + request.getSlug());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> AppException.notFound("Danh mục không tồn tại"));

        product.setSku(request.getSku()); product.setName(request.getName());
        product.setSlug(request.getSlug()); product.setDescription(request.getDescription());
        product.setPrice(request.getPrice()); product.setSalePrice(request.getSalePrice());
        product.setCategory(category); product.setBrand(request.getBrand());
        product.setWeightGram(request.getWeightGram());
        product.setStatus(Product.ProductStatus.valueOf(request.getStatus()));

        // Replace images
        if (request.getImageUrls() != null) {
            product.getImages().clear();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                product.getImages().add(ProductImage.builder().product(product)
                        .url(request.getImageUrls().get(i)).sortOrder(i).isPrimary(i == 0).build());
            }
        }

        // Replace attributes
        if (request.getAttributes() != null) {
            product.getAttributes().clear();
            request.getAttributes().forEach((k, v) ->
                    product.getAttributes().add(ProductAttribute.builder().product(product).attrKey(k).attrValue(v).build()));
        }

        Product saved = productRepository.save(product);
        saveOutboxEvent(saved, productUpdatedTopic);
        return ProductResponse.from(saved);
    }

    @Override
    public void deleteProduct(String id) {
        Product product = findById(id);
        saveOutboxDeleteEvent(product);
        productRepository.delete(product);
    }

    @Override
    public ProductResponse updateStatus(String id, String status) {
        Product product = findById(id);
        product.setStatus(Product.ProductStatus.valueOf(status));
        Product saved = productRepository.save(product);
        saveOutboxEvent(saved, productUpdatedTopic);
        return ProductResponse.from(saved);
    }

    // ── Helpers ──────────────────────────────────────────────

    private Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Sản phẩm không tồn tại"));
    }

    private void saveOutboxEvent(Product product, String topic) {
        try {
            ProductEventPayload payload = ProductEventPayload.builder()
                    .productId(product.getId()).sku(product.getSku()).name(product.getName())
                    .price(product.getPrice()).salePrice(product.getSalePrice())
                    .categoryId(product.getCategory().getId()).status(product.getStatus().name())
                    .eventType(topic).build();
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("PRODUCT").aggregateId(product.getId())
                    .eventType(topic).payload(objectMapper.writeValueAsString(payload)).build());
        } catch (Exception e) {
            log.error("Failed to save outbox event for product {}: {}", product.getId(), e.getMessage());
        }
    }

    private void saveOutboxDeleteEvent(Product product) {
        try {
            Map<String, String> payload = Map.of("productId", product.getId(), "eventType", "product.deleted");
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("PRODUCT").aggregateId(product.getId())
                    .eventType(productDeletedTopic).payload(objectMapper.writeValueAsString(payload)).build());
        } catch (Exception e) {
            log.error("Failed to save outbox delete event for product {}: {}", product.getId(), e.getMessage());
        }
    }
}