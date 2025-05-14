package com.example.backendeco.Controller;

import com.example.backendeco.Entity.Product;
import com.example.backendeco.Entity.RecommendedProduct;
import com.example.backendeco.Service.ProductService;
import com.example.backendeco.dto.ProductDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {"http://localhost:4200", "https://4b95-34-16-150-26.ngrok-free.app"}, allowCredentials = "true")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    @Value("${recommendation.api.url:https://282b-34-70-163-230.ngrok-free.app }")
    private String recommendationApiUrl;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Get all products as DTOs
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        try {
            List<Product> products = productService.findAll();
            List<ProductDTO> dtos = products.stream()
                    .map(ProductDTO::new)
                    .collect(Collectors.toList());
            logger.info("Fetched {} products", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching all products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get product by ID
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        try {
            if (id <= 0) {
                return ResponseEntity.badRequest().body(null);
            }
            Product product = productService.getProductById(id);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(new ProductDTO(product));
        } catch (Exception e) {
            logger.error("Error fetching product with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Add a new product
    @PostMapping
    public ResponseEntity<Product> addProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("categoryId") Long categoryId) {
        try {
            Product product = productService.addProduct(name, description, price, image, categoryId);
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        } catch (IOException e) {
            logger.error("Error processing image for product", e);
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Error adding product", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Delete product by ID
    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting product with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get products by category
    @GetMapping("/category/{categoryId:[0-9]+}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<Product> products = productService.findByCategoryId(categoryId);
            List<ProductDTO> dtos = products.stream()
                    .map(ProductDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching products for category ID: {}", categoryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Update externalId manually (optional)
    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<Product> updateExternalId(@PathVariable Long id, @RequestParam(required = false) String externalId) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) return ResponseEntity.notFound().build();
            product.setExternalId(externalId);
            Product updatedProduct = productService.updateProduct(product);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            logger.error("Error updating externalId for product ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Dataset info endpoint
    @GetMapping("/dataset-info")
    public ResponseEntity<List<Map<String, String>>> getDatasetInfo() {
        try {
            List<Product> products = productService.findAll();
            List<Map<String, String>> result = products.stream().map(p -> {
                Map<String, String> map = new HashMap<>();
                map.put("name", p.getName());
                map.put("externalId", Optional.ofNullable(p.getExternalId()).orElse(""));
                map.put("imageUrl", Optional.ofNullable(p.getImageUrl()).orElse(""));
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching dataset info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get recommendations using ProductService
    @GetMapping("/{id:[0-9]+}/recommendations")
    public ResponseEntity<List<RecommendedProduct>> getRecommendations(@PathVariable Long id) {
        try {
            List<RecommendedProduct> recommendations = productService.getRecommendations(id);
            return ResponseEntity.ok(recommendations);
        } catch (IOException e) {
            // Network issue or API unreachable
            System.err.println("IO Error fetching recommendations for product ID " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());
        } catch (InterruptedException e) {
            // Thread interrupted during HTTP call
            System.err.println("Request interrupted while fetching recommendations for product ID " + id + ": " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Collections.emptyList());
        } catch (Exception e) {
            // Any other unexpected errors
            System.err.println("Unexpected error fetching recommendations for product ID " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    // Serve image bytes
    @GetMapping("/{id:[0-9]+}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            if (product == null || product.getImage() == null) {
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(product.getImage(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching image for product ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}