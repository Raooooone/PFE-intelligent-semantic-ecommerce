package com.example.backendeco.Controller;

import com.example.backendeco.Service.OrderService;
import com.example.backendeco.dto.OrderDTO;
import com.example.backendeco.Entity.Order;
import com.example.backendeco.dto.OrderItemDTO;
import com.example.backendeco.dto.ProductDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger logger = Logger.getLogger(OrderController.class.getName());
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * On reçoit un OrderDTO, on crée la commande et on renvoie
     * éventuellement un OrderDTO enrichi ou l'entité Order (à adapter).
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        logger.info("Received OrderDTO: " + orderDTO);
        // on appelle bien la méthode qui attend un OrderDTO
        Order savedOrder = orderService.createOrder(orderDTO);

        // Si vous avez besoin de reconvertir en DTO pour la réponse :
        OrderDTO savedDto = mapToDto(savedOrder);

        return ResponseEntity
                .status(201)
                .body(savedDto);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> list = orderService.getAllOrders()
                .stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        logger.info("Deleted order with ID: " + id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Petite méthode utilitaire pour convertir Order → OrderDTO.
     * Implémentez-la avec vos champs et la liste d'items.
     */
    private OrderDTO mapToDto(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setPrenom(order.getPrenom());
        dto.setNom(order.getNom());
        dto.setAdresse(order.getAdresse());
        dto.setVille(order.getVille());
        dto.setCodePostal(order.getCodePostal());
        dto.setPays(order.getPays());
        dto.setTelephone(order.getTelephone());
        dto.setEmail(order.getEmail());
        dto.setNotes(order.getNotes());
        dto.setSousTotal(order.getSousTotal());
        dto.setShipping(order.getShipping());
        dto.setTotal(order.getTotal());
        dto.setDateCommande(order.getDateCommande());
        // convertir la liste OrderItem → OrderItemDTO
        List<OrderItemDTO> items = order.getItems().stream().map(item -> {
            OrderItemDTO iDto = new OrderItemDTO();
            iDto.setProduct(new ProductDTO(item.getProduct()));
            iDto.setQuantity(item.getQuantity());
            return iDto;
        }).toList();
        dto.setItems(items);
        return dto;
    }
}
