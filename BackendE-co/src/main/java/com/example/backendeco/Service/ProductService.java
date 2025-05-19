package com.example.backendeco.Service;

import com.example.backendeco.Entity.Category;
import com.example.backendeco.Entity.Product;
import com.example.backendeco.Entity.RecommendedProduct;
import com.example.backendeco.Repository.CategoryRepository;
import com.example.backendeco.Repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Random random = new Random();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${recommendation.api.url:https://66d7-34-150-202-174.ngrok-free.app}")
    private String recommendationApiUrl;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // === CRUD Methods ===
    public List<Product> findAll() {
        try {
            List<Product> products = productRepository.findAll();
            logger.info("Fetched {} products", products.size());
            return products;
        } catch (Exception e) {
            logger.error("Error fetching all products: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch all products", e);
        }
    }

    public Product addProduct(String name, String description, Double price, MultipartFile imageFile, Long categoryId, String externalId) throws IOException {
        try {
            byte[] imageData = (imageFile != null && !imageFile.isEmpty()) ? imageFile.getBytes() : null;
            return addProduct(name, description, price, imageData, categoryId, externalId);
        } catch (IOException e) {
            logger.error("Error processing image file for product '{}': {}", name, e.getMessage(), e);
            throw e;
        }
    }

    public Product addProduct(String name, String description, Double price, byte[] image, Long categoryId, String externalId) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setImage(image);
            product.setExternalId(externalId); // Set the externalId

            if (image != null && image.length <= 1024 * 1024) {
                product.setImageUrl("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image));
            } else {
                product.setImageUrl("");
            }

            product.setCategory(category);
            int generatedStock = random.nextInt(101);
            product.setStock(generatedStock);
            logger.info("Generated stock for '{}': {}", name, generatedStock);

            Product savedProduct = productRepository.save(product);
            logger.info("Saved product with ID: {}", savedProduct.getId());
            return savedProduct;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid category for product '{}': {}", name, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error saving product '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to save product", e);
        }
    }

    public Product updateProduct(Product product) {
        try {
            Product updatedProduct = productRepository.save(product);
            logger.info("Updated product with ID: {}", updatedProduct.getId());
            return updatedProduct;
        } catch (Exception e) {
            logger.error("Error updating product ID {}: {}", product.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to update product", e);
        }
    }

    public void deleteProduct(Long id) {
        try {
            productRepository.deleteById(id);
            logger.info("Deleted product with ID: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting product with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete product", e);
        }
    }

    public Product getProductById(Long id) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                logger.warn("Product not found for ID: {}", id);
            } else {
                logger.info("Fetched product with ID: {}", id);
            }
            return product;
        } catch (Exception e) {
            logger.error("Error fetching product with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch product", e);
        }
    }

    public List<Product> findByCategoryId(Long categoryId) {
        try {
            List<Product> products = productRepository.findAllByCategoryId(categoryId);
            logger.info("Fetched {} products for category ID: {}", products.size(), categoryId);
            return products;
        } catch (Exception e) {
            logger.error("Error fetching products for category ID {}: {}", categoryId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch products by category", e);
        }
    }

    @Transactional
    public Product updateStock(Long id, Integer newStock) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

            if (newStock < 0) {
                logger.error("Attempted to update stock with a negative value for product ID: {}", id);
                throw new IllegalArgumentException("Stock cannot be negative.");
            }

            product.setStock(newStock);
            Product updatedProduct = productRepository.save(product);
            logger.info("Updated stock for product ID: {} to new value: {}", id, newStock);
            return updatedProduct;
        } catch (IllegalArgumentException e) {
            logger.error("Validation error while updating stock for product ID: {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating stock for product ID: {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to update stock", e);
        }
    }

    public Product findByExternalId(String externalId) {
        try {
            Product product = productRepository.findByExternalId(externalId).orElse(null);
            if (product == null) {
                logger.warn("No product found for externalId: {}", externalId);
            } else {
                logger.info("Fetched product for externalId: {}", externalId);
            }
            return product;
        } catch (Exception e) {
            logger.error("Error fetching product for externalId {}: {}", externalId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch product by externalId", e);
        }
    }

    // === PostConstruct Methods ===
    @PostConstruct
    public void initExistingStocks() {
        try {
            List<Product> toUpdate = productRepository.findAll().stream()
                    .filter(p -> p.getStock() == null || p.getStock() == 0)
                    .peek(p -> {
                        int newStock = random.nextInt(101);
                        p.setStock(newStock);
                        logger.info("Initialized stock for product ID {} to {}", p.getId(), newStock);
                    })
                    .collect(Collectors.toList());

            if (!toUpdate.isEmpty()) {
                productRepository.saveAll(toUpdate);
                logger.info("Updated stock for {} products", toUpdate.size());
            } else {
                logger.info("No products needed stock initialization");
            }
        } catch (Exception e) {
            logger.error("Error initializing stocks: {}", e.getMessage(), e);
        }
    }

    @PostConstruct
    public void initImageUrls() {
        try {
            List<Product> products = productRepository.findAll();
            logger.info("Total products found: {}", products.size());

            List<Product> toUpdate = products.stream()
                    .peek(p -> {
                        if (p.getImageUrl() == null) {
                            if (p.getImage() != null && p.getImage().length <= 1024 * 1024) {
                                p.setImageUrl("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(p.getImage()));
                                logger.info("Initialized imageUrl for product ID: {}", p.getId());
                            } else {
                                p.setImageUrl("");
                                logger.info("Set empty imageUrl for product ID: {} (no image or image too large)", p.getId());
                            }
                        } else {
                            logger.info("Product ID: {} already has imageUrl: {}", p.getId(), p.getImageUrl());
                        }
                    })
                    .filter(p -> p.getImageUrl() != null)
                    .collect(Collectors.toList());

            if (!toUpdate.isEmpty()) {
                productRepository.saveAll(toUpdate);
                logger.info("Updated imageUrl for {} products", toUpdate.size());
            } else {
                logger.info("No products needed imageUrl initialization");
            }
        } catch (Exception e) {
            logger.error("Error initializing imageUrls: {}", e.getMessage(), e);
        }
    }

    @PostConstruct
    public void initExternalIds() {
        try {
            List<Product> allProducts = productRepository.findAll();

            if (allProducts.isEmpty()) {
                logger.info("No products found in DB");
                return;
            }

            List<Product> toUpdate = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.getExternalId() == null || p.getExternalId().trim().isEmpty()) {
                    String assignedId = "EXT" + p.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
                    p.setExternalId(assignedId);
                    logger.info("Assigned externalId: {} to AscendingFormat for product ID: {}", assignedId, p.getId());
                    toUpdate.add(p);
                }
            }

            if (!toUpdate.isEmpty()) {
                productRepository.saveAll(toUpdate);
                logger.info("Updated externalId for {} products", toUpdate.size());
            } else {
                logger.info("No products needed externalId initialization");
            }
        } catch (Exception e) {
            logger.error("Error initializing externalIds: {}", e.getMessage(), e);
        }
    }

    // === Recommendation Method ===
    public List<RecommendedProduct> getRecommendations(Long productId) throws IOException, InterruptedException {
        try {
            if (productId == null || productId <= 0) {
                logger.error("Invalid product ID: {}", productId);
                throw new IllegalArgumentException("Invalid product ID: " + productId);
            }

            Product product = getProductById(productId);
            if (product == null) {
                logger.warn("Product not found for ID: {}", productId);
                throw new IllegalArgumentException("Product not found for ID: " + productId);
            }

            String safeRecommendationUrl = recommendationApiUrl.trim();
            String apiUrl;
            HttpRequest request;

            boolean isNewProduct = product.getExternalId() == null || product.getExternalId().trim().isEmpty();
            if (!isNewProduct) {
                String externalId = product.getExternalId().trim();
                apiUrl = String.format("%s/recommend/%s?top_n=5", safeRecommendationUrl, externalId);
                logger.info("Calling recommendation API at: {}", apiUrl);

                request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .GET()
                        .header("Accept", "application/json")
                        .build();
            } else {
                apiUrl = String.format("%s/recommend_external?top_n=5", safeRecommendationUrl);
                logger.info("Calling recommendation API for new product at: {}", apiUrl);

                ObjectNode payload = objectMapper.createObjectNode();
                String imageUrl = product.getImageUrl() != null ? product.getImageUrl() : "";
                String description = product.getDescription() != null ? product.getDescription() : product.getName();
                payload.put("img_path", imageUrl);
                payload.put("description", description);
                String jsonPayload = objectMapper.writeValueAsString(payload);
                logger.debug("POST payload: {}", jsonPayload);

                request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();
            }

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            logger.info("Recommendation API response status: {}", statusCode);
            logger.debug("Response body: {}", response.body());

            if (statusCode == 200) {
                RecommendedProduct[] recommendationsArray;
                try {
                    recommendationsArray = objectMapper.readValue(response.body(), RecommendedProduct[].class);
                } catch (Exception e) {
                    logger.error("Error parsing response JSON: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to parse recommendation API response", e);
                }

                List<RecommendedProduct> recommendations = Arrays.stream(recommendationsArray)
                        .peek(rec -> {
                            String recProductId = rec.getProduct_id();
                            logger.debug("Processing recommendation for product_id: {}", recProductId);
                            Product dbProduct = findByExternalId(recProductId);
                            if (dbProduct != null) {
                                rec.setProduct_name(dbProduct.getName());
                                rec.setImg_link(dbProduct.getImageUrl());
                            } else {
                                rec.setProduct_name("Unknown Product");
                                rec.setImg_link("https://example.com/image-not-found.jpg");
                            }
                        })
                        .collect(Collectors.toList());

                logger.info("Received {} recommendations for product ID: {}", recommendations.size(), productId);
                return recommendations;
            } else if (statusCode == 404 && !isNewProduct) {
                logger.info("No recommendations found for externalId: {}. Falling back to new product recommendation.", product.getExternalId());
                apiUrl = String.format("%s/recommend_external?top_n=5", safeRecommendationUrl);

                ObjectNode payload = objectMapper.createObjectNode();
                String imageUrl = product.getImageUrl() != null ? product.getImageUrl() : "";
                String description = product.getDescription() != null ? product.getDescription() : product.getName();
                payload.put("img_path", imageUrl);
                payload.put("description", description);
                String jsonPayload = objectMapper.writeValueAsString(payload);
                logger.debug("Fallback POST payload: {}", jsonPayload);

                request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();

                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();
                logger.info("Recommendation API response status (fallback): {}", statusCode);
                logger.debug("Fallback response body: {}", response.body());

                if (statusCode == 200) {
                    RecommendedProduct[] recommendationsArray;
                    try {
                        recommendationsArray = objectMapper.readValue(response.body(), RecommendedProduct[].class);
                    } catch (Exception e) {
                        logger.error("Error parsing fallback response JSON: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to parse fallback recommendation API response", e);
                    }

                    List<RecommendedProduct> recommendations = Arrays.stream(recommendationsArray)
                            .peek(rec -> {
                                String recProductId = rec.getProduct_id();
                                logger.debug("Processing fallback recommendation for product_id: {}", recProductId);
                                Product dbProduct = findByExternalId(recProductId);
                                if (dbProduct != null) {
                                    rec.setProduct_name(dbProduct.getName());
                                    rec.setImg_link(dbProduct.getImageUrl());
                                } else {
                                    rec.setProduct_name("Unknown Product");
                                    rec.setImg_link("https://example.com/image-not-found.jpg");
                                }
                            })
                            .collect(Collectors.toList());

                    logger.info("Received {} recommendations for new product ID: {}", recommendations.size(), productId);
                    return recommendations;
                }
            }

            if (statusCode == 404) {
                logger.info("No recommendations found for product ID: {}", productId);
                return getDefaultRecommendations(product);
            } else if (statusCode == 400) {
                logger.error("Invalid request for product ID: {}", productId);
                throw new IllegalArgumentException("Invalid request for product ID: " + productId);
            } else {
                logger.warn("API failed with status {}. Falling back to default recommendations.", statusCode);
                return getDefaultRecommendations(product);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error calling recommendation API for product ID: {}: {}", productId, e.getMessage(), e);
            return getDefaultRecommendations(getProductById(productId));
        } catch (Exception e) {
            logger.error("Error fetching recommendations for product ID: {}: {}", productId, e.getMessage(), e);
            return getDefaultRecommendations(getProductById(productId));
        }
    }

    // Helper method to provide default recommendations
    private List<RecommendedProduct> getDefaultRecommendations(Product product) {
        logger.info("Providing default recommendations for product ID: {}", product != null ? product.getId() : "null");
        if (product == null || product.getCategory() == null) {
            logger.warn("Product or category is null. Returning empty recommendations.");
            return List.of();
        }

        logger.info("Product category: {}", product.getCategory().getName());
        // Fetch top 5 products from the same category, sorted by stock (popularity proxy)
        List<Product> sameCategoryProducts = productRepository.findAllByCategoryId(product.getCategory().getId())
                .stream()
                .filter(p -> !p.getId().equals(product.getId())) // Exclude the current product
                .sorted((p1, p2) -> Integer.compare(p2.getStock(), p1.getStock())) // Sort by stock descending
                .limit(5)
                .collect(Collectors.toList());

        // Convert to RecommendedProduct objects
        List<RecommendedProduct> defaultRecommendations = sameCategoryProducts.stream()
                .map(p -> {
                    RecommendedProduct rec = new RecommendedProduct();
                    rec.setProduct_id(p.getExternalId() != null ? p.getExternalId() : "EXT" + p.getId());
                    rec.setCategory(p.getCategory().getName());
                    rec.setProduct_name(p.getName());
                    rec.setImg_link(p.getImageUrl() != null ? p.getImageUrl() : "https://example.com/image-not-found.jpg");
                    return rec;
                })
                .collect(Collectors.toList());

        logger.info("Generated {} default recommendations for product ID: {}", defaultRecommendations.size(), product.getId());
        return defaultRecommendations;
    }
}