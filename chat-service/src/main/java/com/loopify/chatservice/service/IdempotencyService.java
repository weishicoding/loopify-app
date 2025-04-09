package com.loopify.chatservice.service;

import com.loopify.chatservice.model.ProcessedMessage;
import com.loopify.chatservice.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedMessageRepository processedMessageRepository;

    public boolean isProcessed(String messageId) {
        return processedMessageRepository.existsById(messageId);
    }

    public void markAsProcessed(String messageId) {
        if (!isProcessed(messageId)) {
            ProcessedMessage processed = ProcessedMessage.builder().id(messageId).build();
            processedMessageRepository.save(processed);
        }
    }
}
