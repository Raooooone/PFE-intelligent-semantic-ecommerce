package com.example.backendeco.dto;

import com.example.backendeco.Entity.Product;

public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    private String imageUrl;
    private Integer stock;
    private String description;
    private String externalId;
    private String image;

    public ProductDTO() {}

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.imageUrl = product.getImageUrl();
        this.stock = product.getStock();
        this.description = product.getDescription();
        this.externalId = product.getExternalId();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
