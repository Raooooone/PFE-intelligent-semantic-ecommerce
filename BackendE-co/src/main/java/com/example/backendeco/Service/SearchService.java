package com.example.backendeco.Service;
import com.example.backendeco.Entity.SearchRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SearchService {

    private final RestTemplate restTemplate;

    public SearchService() {
        this.restTemplate = new RestTemplate();
    }

    public String searchProducts(String query) {
        String pythonApiUrl = "https://4b95-34-16-150-26.ngrok-free.app/search";

        // Création du corps JSON
        SearchRequest requestBody = new SearchRequest(query, 5, 0.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SearchRequest> request = new HttpEntity<>(requestBody, headers);

        // Appel POST vers FastAPI
        ResponseEntity<String> response = restTemplate.exchange(
                pythonApiUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }
}

