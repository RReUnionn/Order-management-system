package com.vupl.productservice.service;

import com.vupl.productservice.dto.request.CategoryRequest;
import com.vupl.productservice.dto.request.ProductRequest;
import com.vupl.productservice.dto.request.ProductSearchRequest;
import com.vupl.productservice.dto.response.CategoryResponse;
import com.vupl.productservice.dto.response.PageResponse;
import com.vupl.productservice.dto.response.ProductResponse;

import java.util.List;
public interface ProductService {
    // Category
    List<CategoryResponse> getRootCategories();
    List<CategoryResponse> getChildCategories(String parentId);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(String id, CategoryRequest request);
    void deleteCategory(String id);

    // Product
    PageResponse<ProductResponse> searchProducts(ProductSearchRequest request);
    ProductResponse getProductById(String id);
    ProductResponse getProductBySlug(String slug);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(String id, ProductRequest request);
    void deleteProduct(String id);
    ProductResponse updateStatus(String id, String status);
}
