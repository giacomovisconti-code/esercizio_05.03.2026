package org.example.orderservice.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

//    @Value("{kafka.topic.send.message:send-confirm-message}")
    private String MESSAGE_TOPIC = "send-confirm-message";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public  void sendMessage(String fakeConfirm){
        System.out.println("Sending");
        kafkaTemplate.send(MESSAGE_TOPIC,fakeConfirm);
        System.out.println("Sended");
    }

}
