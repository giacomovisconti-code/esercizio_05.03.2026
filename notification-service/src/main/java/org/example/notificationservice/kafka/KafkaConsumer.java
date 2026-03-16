package org.example.notificationservice.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {


    @KafkaListener(topics = "send-confirm-message", groupId = "message-consumer-group")
    public void listenForOrder(String message){
        System.out.println("Messaggio ricevuto: " + message);
    }
}
