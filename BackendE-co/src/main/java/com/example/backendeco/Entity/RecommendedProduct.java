package com.example.backendeco.Entity;

public class RecommendedProduct {
    private String product_id;
    private String category;
    private String product_name;
    private String img_link;

    // Getters and setters
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getProduct_name() { return product_name; }
    public void setProduct_name(String product_name) { this.product_name = product_name; }

    public String getImg_link() { return img_link; }
    public void setImg_link(String img_link) { this.img_link = img_link; }
}