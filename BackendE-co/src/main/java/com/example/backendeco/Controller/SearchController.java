package com.example.backendeco.Controller;
import com.example.backendeco.Entity.SearchRequest;
import com.example.backendeco.Service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String query) {
        String result = searchService.searchProducts(query);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestBody SearchRequest searchRequest) {
        String result = searchService.searchProducts(searchRequest.getQuery());
        return ResponseEntity.ok(result);
    }

}

