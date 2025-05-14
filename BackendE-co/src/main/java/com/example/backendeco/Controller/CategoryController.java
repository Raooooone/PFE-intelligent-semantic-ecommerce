package com.example.backendeco.Controller;

import com.example.backendeco.Entity.Category;
import com.example.backendeco.Entity.Product;
import com.example.backendeco.Service.CategoryService;
import com.example.backendeco.Service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.findById(id);
        return category != null ? ResponseEntity.ok(category) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        return categoryService.save(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/string", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCategoryPathsAsString() {
        try {
            String categoryPaths = categoryService.getCategoryPathsAsString();
            if (categoryPaths.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(categoryPaths);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating category paths: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long id) {
        Category category = categoryService.findById(id);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        List<Product> products = productService.findByCategoryId(id);
        return ResponseEntity.ok(products);
    }
}