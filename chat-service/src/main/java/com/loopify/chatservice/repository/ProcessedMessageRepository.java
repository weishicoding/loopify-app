package com.loopify.chatservice.repository;

import com.loopify.chatservice.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {

    boolean existsById(String id);
}
