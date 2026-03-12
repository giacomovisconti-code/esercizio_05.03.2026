package org.example.orderservice.controllers;

import org.example.orderservice.dto.order.ItemToOrder;
import org.example.orderservice.entities.Order;
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
    public ResponseEntity<Order> getOrderById(@PathVariable("id") UUID id){
        return ResponseEntity.ok(orderService.getSingleOrderById(id));
    }

    //? CREATE
    @PostMapping("/create/{userId}")
    public ResponseEntity<String> createOrder(@RequestBody List<ItemToOrder> itemList, @PathVariable("userId") UUID userId) throws Exception {
        orderService.createOrder(itemList, userId);
        return ResponseEntity.ok("Ordine creato con successo!");
    }



}
