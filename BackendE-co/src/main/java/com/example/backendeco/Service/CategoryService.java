package com.example.backendeco.Service;

import com.example.backendeco.Entity.Category;
import com.example.backendeco.Repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    public String getCategoryPathsAsString() {
        List<Category> categories = findAll();
        return categories.stream()
                .map(Category::getName) // Get the name field (the full path)
                .filter(path -> path != null && !path.isEmpty()) // Filter out null or empty paths
                .collect(Collectors.joining(",")); // Join with commas
    }
}