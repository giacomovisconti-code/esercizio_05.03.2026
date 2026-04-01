package org.example.notificationservice.services;

import org.example.notificationservice.entities.Message;
import org.example.notificationservice.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    //? INDEX
    public List<Message> getAllMessages(){
        return messageRepository.findAll();
    }

    //? SHOW
    public Message getMessageFromOrderId(UUID orderId){
        if (orderId == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inserire un Order id per effettuare la ricerca");
        }

        return messageRepository.findByMessageTextContaining(orderId.toString()).orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Messaggio non trovato"));
    }
}
