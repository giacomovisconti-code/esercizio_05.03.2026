package org.example.notificationservice.kafka;

import org.example.notificationservice.entities.Message;
import org.example.notificationservice.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @Autowired
    private MessageRepository messageRepository;

    @KafkaListener(topics = "send-confirm-message", groupId = "message-consumer-group")
    public void listenForOrder(String message){

        // Mando il messaggio
        System.out.println("Messaggio ricevuto: " + message);

        // Salvo il messaggio inviato
        Message m = new Message();
        m.setMessageText(message);
        messageRepository.save(m);
    }
}
