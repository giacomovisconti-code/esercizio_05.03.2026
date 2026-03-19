package org.example.orderservice.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import org.example.orderservice.dto.order.ItemToOrder;
import org.example.orderservice.entities.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    //? INDEX
    @GetMapping()
    public ResponseEntity<List<Order>> getAllOrders(){
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    //? SHOW
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@NotNull @PathVariable("id") UUID id, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("role") String role) throws Exception {
        return ResponseEntity.ok(orderService.getSingleOrderById(id, userId, role));
    }

    //? CREATE
    @PostMapping("/create")
    public ResponseEntity<String> createOrder(@RequestBody List<@Valid ItemToOrder> itemList, @RequestHeader("X-User-Id") UUID userId) throws Exception {
        orderService.createOrder(itemList, userId);
        return ResponseEntity.ok("Ordine creato con successo!");
    }

    //? UPDATE
    @PatchMapping("/update/{orderId}")
    public ResponseEntity<String> updateOrder(@NotNull @PathVariable("orderId") UUID orderId, @RequestBody List<@Valid ItemToOrder> itemList) throws Exception {
        orderService.updateOrder(orderId, itemList);
        return ResponseEntity.ok("Ordine modificato con successo");

    }

    //? CHANGE ORDER STATUS
    @PatchMapping("/changestatus/{orderId}")
    public ResponseEntity<String> changeStatusOrder(@NotNull @PathVariable("orderId") UUID orderId, @RequestParam("status") String status){
        orderService.changeOrderStatus(orderId, status);
        return ResponseEntity.ok("Stato ordine modificato con successo");
    }

    //? DEACTIVE ORDER
    @PatchMapping("/deactivate/{orderId}")
    public ResponseEntity<String> deactivateOrder(@NotNull @PathVariable("orderId") UUID orderId) throws Exception {
        orderService.deactiveOrder(orderId);
        return ResponseEntity.ok("Ordine Disattivato con successo!");
    }
    //? REACTIVE ORDER
    @PatchMapping("/reactivate/{orderId}")
    public ResponseEntity<String> reactivateOrder(@NotNull @PathVariable("orderId") UUID orderId) throws Exception {
        orderService.reactivateOrder(orderId);
        return ResponseEntity.ok("Ordine Riattivato con successo!");
    }

    //? SOFT DELETE ORDER
    @PatchMapping("/delete/{orderId}")
    public ResponseEntity<String> deleteOrder(@NotNull @PathVariable("orderId") UUID orderId) throws Exception {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok("Ordine eliminato con successo");
    }
}
