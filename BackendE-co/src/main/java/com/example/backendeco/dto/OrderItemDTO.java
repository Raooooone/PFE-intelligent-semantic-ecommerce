package com.example.backendeco.dto;

public class OrderItemDTO {
    private Long id; // Added to support OrderItem identification in responses
    private ProductDTO product; // Reference to the product (minimal structure)
    private Integer quantity; // Quantity of the product in the order

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}