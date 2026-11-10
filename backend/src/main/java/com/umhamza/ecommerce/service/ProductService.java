
package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.ProductRequest;
import com.umhamza.ecommerce.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long productId);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long productId, ProductRequest request);

    void deleteProduct(Long productId);
}
