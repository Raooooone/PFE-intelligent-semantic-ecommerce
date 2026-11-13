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
@CrossOrigin(origins = {"http://localhost:4200", "https://ce6b-34-10-59-211.ngrok-free.app"}, allowCredentials = "true")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    @Value("${recommendation.api.url:https://66d7-34-150-202-174.ngrok-free.app}")
    private String recommendationApiUrl;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        logger.info("Received request to fetch all products");
        List<Product> products = productService.findAll();
        List<ProductDTO> dtos = products.stream()
                .map(product -> {
                    ProductDTO dto = new ProductDTO(product);
                    if (product.getImage() != null) {
                        dto.setImage(Base64.getEncoder().encodeToString(product.getImage()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        logger.info("Fetched {} products", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        try {
            if (id <= 0) {
                logger.warn("Invalid product ID provided: {}", id);
                return ResponseEntity.badRequest().body(null);
            }
            Product product = productService.getProductById(id);
            if (product == null) {
                logger.warn("Product not found for ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(new ProductDTO(product));
        } catch (Exception e) {
            logger.error("Error fetching product with ID: {} - {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Product> addProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "externalId", required = false) String externalId) {
        try {
            Product product = productService.addProduct(name, description, price, image, categoryId, externalId);
            logger.info("Successfully added product with ID: {}", product.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        } catch (IOException e) {
            logger.error("Error processing image for product - {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Error adding product - {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            logger.info("Successfully deleted product with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting product with ID: {} - {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/category/{categoryId:[0-9]+}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<Product> products = productService.findByCategoryId(categoryId);
            List<ProductDTO> dtos = products.stream()
                    .map(ProductDTO::new)
                    .collect(Collectors.toList());
            logger.info("Fetched {} products for category ID: {}", dtos.size(), categoryId);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching products for category ID: {} - {}", categoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<Product> updateExternalId(@PathVariable Long id, @RequestParam(required = false) String externalId) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                logger.warn("Product not found for ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            product.setExternalId(externalId);
            Product updatedProduct = productService.updateProduct(product);
            logger.info("Updated externalId for product ID: {} to {}", id, externalId);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            logger.error("Error updating externalId for product ID: {} - {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/sync-external-ids")
    public ResponseEntity<String> syncExternalIds(@RequestBody List<Map<String, String>> externalIdMapping) {
        try {
            int updatedCount = 0;
            for (Map<String, String> mapping : externalIdMapping) {
                String externalId = mapping.get("externalId");
                Long localId = Long.parseLong(mapping.get("localId"));
                Product product = productService.getProductById(localId);
                if (product != null) {
                    product.setExternalId(externalId);
                    productService.updateProduct(product);
                    updatedCount++;
                }
            }
            logger.info("Successfully synchronized {} external IDs", updatedCount);
            return ResponseEntity.ok("Successfully synchronized " + updatedCount + " external IDs");
        } catch (Exception e) {
            logger.error("Error synchronizing external IDs - {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error synchronizing external IDs: " + e.getMessage());
        }
    }

    @GetMapping("/dataset-info")
    public ResponseEntity<List<Map<String, String>>> getDatasetInfo() {
        try {
            List<Product> products = productService.findAll();
            List<Map<String, String>> result = products.stream().map(p -> {
                Map<String, String> map = new HashMap<>();
                map.put("name", p.getName());
                map.put("externalId", Optional.ofNullable(p.getExternalId()).orElse(""));
                map.put("imageUrl", Optional.ofNullable(p.getImageUrl()).orElse(""));
                map.put("localId", String.valueOf(p.getId()));
                return map;
            }).collect(Collectors.toList());
            logger.info("Fetched dataset info for {} products", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching dataset info - {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id:[0-9]+}/recommendations")
    public ResponseEntity<List<RecommendedProduct>> getRecommendations(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                logger.warn("Product not found for ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            String externalId = product.getExternalId();
            if (externalId == null || externalId.isEmpty()) {
                logger.warn("No externalId found for product ID: {}", id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
            }
            List<RecommendedProduct> recommendations = productService.getRecommendations(id);
            if (recommendations.isEmpty()) {
                logger.warn("No recommendations found for product ID: {}", id);
                return ResponseEntity.ok(Collections.emptyList());
            }
            logger.info("Fetched {} recommendations for product ID: {}", recommendations.size(), id);
            return ResponseEntity.ok(recommendations);
        } catch (IOException e) {
            logger.error("IO Error fetching recommendations for product ID {} - {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());
        } catch (InterruptedException e) {
            logger.error("Request interrupted while fetching recommendations for product ID {} - {}", id, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Unexpected error fetching recommendations for product ID {} - {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/{id:[0-9]+}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            if (product == null || product.getImage() == null) {
                logger.warn("Image not found for product ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            logger.info("Successfully fetched image for product ID: {}", id);
            return new ResponseEntity<>(product.getImage(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching image for product ID: {} - {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/external/{externalId}")
    public ResponseEntity<Map<String, Long>> getProductIdByExternalId(@PathVariable String externalId) {
        try {
            Product product = productService.findByExternalId(externalId);
            if (product == null) {
                logger.warn("No product found for externalId: {}", externalId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            Map<String, Long> response = new HashMap<>();
            response.put("id", product.getId());
            logger.info("Fetched product ID {} for externalId: {}", product.getId(), externalId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching product ID for externalId: {} - {}", externalId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ProductDTO> updateStock(@PathVariable Long id, @RequestBody Integer newStock) {
        try {
            Product updatedProduct = productService.updateStock(id, newStock);
            if (updatedProduct == null) {
                logger.warn("Product not found for ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            logger.info("Updated stock for product ID: {} to {}", id, newStock);
            return ResponseEntity.ok(new ProductDTO(updatedProduct));
        } catch (Exception e) {
            logger.error("Error updating stock for product ID: {} - {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}