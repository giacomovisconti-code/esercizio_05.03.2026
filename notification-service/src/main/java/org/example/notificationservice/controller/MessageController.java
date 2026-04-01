package org.example.notificationservice.controller;

import org.example.notificationservice.entities.Message;
import org.example.notificationservice.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping
    public ResponseEntity<List<Message>> getAllMessages(){
        return ResponseEntity.ok(messageService.getAllMessages());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Message> getMessage(@PathVariable("orderId") UUID orderId) {
        return ResponseEntity.ok(messageService.getMessageFromOrderId(orderId));
    }


}
