package org.example.orderservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    public  void sendMessage(String fakeConfirm){
        //    @Value("{kafka.topic.send.message:send-confirm-message}")
        String MESSAGE_TOPIC = "send-confirm-message";
        kafkaTemplate.send(MESSAGE_TOPIC,fakeConfirm)
                .whenComplete((result, ex) -> {
                    if(ex != null){
                        logger.error("Failed to sent message event:{}", ex.getMessage());
                    } else {
                        logger.info("Message sent successfully to offset: {}", result.getRecordMetadata().offset());
                    }
                });
    }

}
