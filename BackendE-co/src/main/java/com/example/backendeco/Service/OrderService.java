package com.example.backendeco.Service;

import com.example.backendeco.Entity.Order;
import com.example.backendeco.Repository.OrderRepository;
import com.example.backendeco.Repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.backendeco.dto.OrderDTO;
import com.example.backendeco.dto.OrderItemDTO;

import com.example.backendeco.Entity.OrderItem;
import com.example.backendeco.Entity.Product;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }


    @Transactional
    public Order createOrder(OrderDTO orderDTO) {
        Order order = new Order();
        order.setPrenom(orderDTO.getPrenom());
        order.setNom(orderDTO.getNom());
        order.setAdresse(orderDTO.getAdresse());
        order.setVille(orderDTO.getVille());
        order.setCodePostal(orderDTO.getCodePostal());
        order.setPays(orderDTO.getPays());
        order.setTelephone(orderDTO.getTelephone());
        order.setEmail(orderDTO.getEmail());
        order.setNotes(orderDTO.getNotes());
        order.setSousTotal(orderDTO.getSousTotal());
        order.setShipping(orderDTO.getShipping());
        order.setTotal(orderDTO.getTotal());
        order.setDateCommande(new Date());

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDTO.getProduct().getId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setOrder(order);
            orderItems.add(orderItem);

            // Check and decrement stock
            if (product.getStock() < itemDTO.getQuantity()) {
                String message = "Stock insuffisant pour le produit " + product.getName();
                logger.error(message);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
            product.setStock(product.getStock() - itemDTO.getQuantity());
            productRepository.save(product);
            logger.info("Stock mis à jour pour le produit : {}", product.getName());
            // Check for stock rupture (stock reaches 0)
            if (product.getStock() == 0) {
                logger.warn("Rupture de stock détectée pour le produit : {}", product.getName());
                // Optionally, you could trigger a notification or update status here
            }
        }

        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        logger.info("Commande sauvegardée avec ID : {}", savedOrder.getId());
        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Commande introuvable avec l'ID : " + id
            );
        }
        orderRepository.deleteById(id);
    }
}