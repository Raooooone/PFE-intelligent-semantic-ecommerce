package com.example.backendeco.Entity;

public class SearchRequest {
    private String query;
    private int top_k;
    private double alpha;

    public SearchRequest(String query, int top_k, double alpha) {
        this.query = query;
        this.top_k = top_k;
        this.alpha = alpha;
    }

    public String getQuery() {
        return query;
    }

    public int getTop_k() {
        return top_k;
    }

    public double getAlpha() {
        return alpha;
    }
}