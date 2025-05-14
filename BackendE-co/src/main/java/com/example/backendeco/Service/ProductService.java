package com.example.backendeco.Service;

import com.example.backendeco.Entity.Category;
import com.example.backendeco.Entity.Product;
import com.example.backendeco.Entity.RecommendedProduct;
import com.example.backendeco.Repository.CategoryRepository;
import com.example.backendeco.Repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Random random = new Random();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${recommendation.api.url:https://4b95-34-16-150-26.ngrok-free.app}")
    private String recommendationApiUrl;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // === Méthodes CRUD pour les produits ===

    public List<Product> findAll() {
        try {
            List<Product> products = productRepository.findAll();
            System.out.println("[ProductService] Fetched all products: " + products.size());
            return products;
        } catch (Exception e) {
            System.err.println("[ProductService] Error fetching all products: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch all products", e);
        }
    }

    public Product addProduct(String name, String description, Double price, MultipartFile imageFile, Long categoryId) throws IOException {
        try {
            byte[] imageData = (imageFile != null && !imageFile.isEmpty()) ? imageFile.getBytes() : null;
            return addProduct(name, description, price, imageData, categoryId);
        } catch (IOException e) {
            System.err.println("[ProductService] Error processing image file for product '" + name + "': " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("[ProductService] Error adding product '" + name + "': " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add product", e);
        }
    }

    public Product addProduct(String name, String description, Double price, byte[] image, Long categoryId) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable avec l'ID: " + categoryId));
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setImage(image);

            if (image != null && image.length <= 1024 * 1024) { // Limite à 1 Mo
                product.setImageUrl("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image));
            } else {
                product.setImageUrl(""); // Frontend utilisera /api/products/{id}/image
            }

            product.setCategory(category);
            int generatedStock = random.nextInt(101);
            product.setStock(generatedStock);
            System.out.println("[ProductService] Generated stock for '" + name + "': " + generatedStock);

            Product savedProduct = productRepository.save(product);
            System.out.println("[ProductService] Saved product: " + savedProduct.getId());

            return savedProduct;
        } catch (IllegalArgumentException e) {
            System.err.println("[ProductService] Invalid category for product '" + name + "': " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("[ProductService] Error saving product '" + name + "': " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save product", e);
        }
    }

    public Product updateProduct(Product product) {
        try {
            Product updatedProduct = productRepository.save(product);
            System.out.println("[ProductService] Updated product: " + updatedProduct.getId());
            return updatedProduct;
        } catch (Exception e) {
            System.err.println("[ProductService] Error updating product ID " + product.getId() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update product", e);
        }
    }

    public void deleteProduct(Long id) {
        try {
            productRepository.deleteById(id);
            System.out.println("[ProductService] Deleted product with ID: " + id);
        } catch (Exception e) {
            System.err.println("[ProductService] Error deleting product with ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete product", e);
        }
    }

    public Product getProductById(Long id) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                System.out.println("[ProductService] Product not found for ID: " + id);
            } else {
                System.out.println("[ProductService] Fetched product: " + id);
            }
            return product;
        } catch (Exception e) {
            System.err.println("[ProductService] Error fetching product with ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch product", e);
        }
    }

    public List<Product> findByCategoryId(Long categoryId) {
        try {
            List<Product> products = productRepository.findAllByCategoryId(categoryId);
            System.out.println("[ProductService] Fetched " + products.size() + " products for category ID: " + categoryId);
            return products;
        } catch (Exception e) {
            System.err.println("[ProductService] Error fetching products for category ID " + categoryId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch products by category", e);
        }
    }

    // === Initialisations PostConstruct ===

    @PostConstruct
    public void initExistingStocks() {
        try {
            List<Product> toUpdate = productRepository.findAll().stream()
                    .filter(p -> p.getStock() == null || p.getStock() == 0)
                    .peek(p -> {
                        int newStock = random.nextInt(101);
                        p.setStock(newStock);
                        System.out.println("[ProductService] Initialized existing product #" + p.getId() + " stock to " + newStock);
                    })
                    .collect(Collectors.toList());

            if (!toUpdate.isEmpty()) {
                productRepository.saveAll(toUpdate);
                System.out.println("[ProductService] Updated stock for " + toUpdate.size() + " products");
            } else {
                System.out.println("[ProductService] No products needed stock initialization");
            }
        } catch (Exception e) {
            System.err.println("[ProductService] Error initializing stocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void initImageUrls() {
        try {
            List<Product> products = productRepository.findAll();
            System.out.println("[ProductService] Total products found: " + products.size());

            List<Product> toUpdate = products.stream()
                    .peek(p -> {
                        if (p.getImageUrl() == null) {
                            if (p.getImage() != null && p.getImage().length <= 1024 * 1024) {
                                p.setImageUrl("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(p.getImage()));
                                System.out.println("[ProductService] Initialized imageUrl for product #" + p.getId());
                            } else {
                                p.setImageUrl("");
                                System.out.println("[ProductService] Set empty imageUrl for product #" + p.getId() + " (no image or image too large)");
                            }
                        } else {
                            System.out.println("[ProductService] Product #" + p.getId() + " already has imageUrl: " + p.getImageUrl());
                        }
                    })
                    .filter(p -> p.getImageUrl() != null)
                    .collect(Collectors.toList());

            if (!toUpdate.isEmpty()) {
                productRepository.saveAll(toUpdate);
                System.out.println("[ProductService] Updated imageUrl for " + toUpdate.size() + " products");
            } else {
                System.out.println("[ProductService] No products needed imageUrl initialization");
            }
        } catch (Exception e) {
            System.err.println("[ProductService] Error initializing imageUrls: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void initExternalIds() {
        try {
            List<Product> allProducts = productRepository.findAll();

            if (allProducts.isEmpty()) {
                System.out.println("[ProductService] No products found in DB");
                return;
            }

            List<String> validExternalIds = allProducts.stream()
                    .map(Product::getExternalId)
                    .filter(Objects::nonNull)
                    .filter(id -> !id.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            System.out.println("[ProductService] Found " + validExternalIds.size() + " valid externalIds in DB");

            if (validExternalIds.isEmpty()) {
                System.out.println("[ProductService] No valid externalIds found in DB, using fallback format");
            }

            int index = 0;
            List<Product> toUpdate = new ArrayList<>();

            for (Product p : allProducts) {
                if (p.getExternalId() == null || p.getExternalId().trim().isEmpty()) {
                    String assignedId;

                    if (index < validExternalIds.size()) {
                        assignedId = validExternalIds.get(index++);
                    } else {
                        assignedId = "EXT" + p.getId(); // Fallback
                    }

                    p.setExternalId(assignedId);
                    System.out.println("[ProductService] Assigned externalId: " + assignedId + " to product ID: " + p.getId());
                    toUpdate.add(p);
                }
            }

            if (!toUpdate.isEmpty()) {
                productRepository.saveAll(toUpdate);
                System.out.println("[ProductService] Updated externalId for " + toUpdate.size() + " products");
            } else {
                System.out.println("[ProductService] No products needed externalId initialization");
            }

        } catch (Exception e) {
            System.err.println("[ProductService] Error initializing externalIds: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // === Méthode pour les recommandations ===

    public List<RecommendedProduct> getRecommendations(Long productId) throws IOException, InterruptedException {
        try {
            if (productId == null || productId <= 0) {
                throw new IllegalArgumentException("Invalid product ID: " + productId);
            }

            Product product = getProductById(productId);
            if (product == null) {
                throw new IllegalArgumentException("Product not found for ID: " + productId);
            }

            if (product.getExternalId() == null || product.getExternalId().trim().isEmpty()) {
                throw new IllegalArgumentException("Product has no externalId for ID: " + productId);
            }

            String externalId = product.getExternalId().trim();
            String safeRecommendationUrl = recommendationApiUrl.trim();
            String apiUrl = String.format("%s/recommend/%s?top_n=5", safeRecommendationUrl, externalId);
            System.out.println("[ProductService] Calling recommendation API at: " + apiUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            System.out.println("[ProductService] Recommendation API response status: " + statusCode);

            if (statusCode == 200) {
                RecommendedProduct[] recommendationsArray = objectMapper.readValue(response.body(), RecommendedProduct[].class);

                List<RecommendedProduct> recommendations = Arrays.stream(recommendationsArray)
                        .peek(rec -> {
                            String recProductId = rec.getProduct_id();
                            List<Product> dbProducts = productRepository.findAllByExternalId(recProductId);

                            if (!dbProducts.isEmpty()) {
                                Product dbProduct = dbProducts.get(0); // Prendre le premier résultat
                                rec.setProduct_name(dbProduct.getName());
                                rec.setImg_link(dbProduct.getImageUrl());
                            } else {
                                rec.setProduct_name("Unknown Product");
                                rec.setImg_link("https://example.com/image-not-found.jpg ");
                            }
                        })
                        .collect(Collectors.toList());

                System.out.println("[ProductService] Received " + recommendations.size() + " recommendations for externalId: " + externalId);
                return recommendations;
            } else if (statusCode == 404) {
                System.out.println("[ProductService] No recommendations found for externalId: " + externalId);
                return List.of(); // Retourner une liste vide si aucune recommandation trouvée
            } else if (statusCode == 400) {
                throw new IllegalArgumentException("Invalid request for externalId: " + externalId);
            } else {
                throw new RuntimeException("Unexpected status from recommendation API: " + statusCode);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[ProductService] Error calling recommendation API for product ID: " + productId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("[ProductService] Error fetching recommendations for product ID: " + productId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch recommendations", e);


        }
    }
}