package com.example.backendeco.Repository;


import com.example.backendeco.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Spring Data JPA générera le bon SQL : SELECT * FROM product WHERE category_id = :categoryId
    List<Product> findAllByCategoryId(Long categoryId);
    Optional<Product> findByExternalId(String externalId);
    List<Product> findAllByExternalIdIn(List<String> externalIds);
    List<Product> findAllByExternalId(String externalId);
}
