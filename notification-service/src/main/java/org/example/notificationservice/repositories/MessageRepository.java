package org.example.notificationservice.repositories;

import org.example.notificationservice.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
