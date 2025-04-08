package com.loopify.mainservice.notification;

import com.loopify.mainservice.enums.NotificationType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notifications}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.follow}")
    private String followRoutingKey;

    @Value("${rabbitmq.routing-key.comment}")
    private String commentRoutingKey;

    // 添加确认回调
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null) {
                if (ack) {
                    log.debug("Message with ID: {} confirmed", correlationData.getId());
                } else {
                    log.error("Message with ID: {} failed to confirm. Reason: {}",
                            correlationData.getId(), cause);
                }
            }
        });

        // Also consider setting return callback for unroutable messages
        rabbitTemplate.setReturnsCallback(returned -> {
            log.warn("Message returned: {}, replyCode: {}, replyText: {}, exchange: {}, routingKey: {}",
                    returned.getMessage().getMessageProperties().getMessageId(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());
        });

        // Enable mandatory returns
        rabbitTemplate.setMandatory(true);
    }

    public void publishNotification(NotificationType notificationType, String jsonPayload, Long messageId) {
        try {
            String routingKey = "";
            if (notificationType == NotificationType.FOLLOW) {
                routingKey = followRoutingKey;
            } else if (notificationType == NotificationType.COMMENT) {
                routingKey = commentRoutingKey;
            }

            // Use the RabbitTemplate's simpler method since your payload is already JSON
            rabbitTemplate.convertAndSend(
                    notificationExchange,
                    routingKey,
                    jsonPayload,
                    message -> {
                        // Set necessary headers on the message
                        message.getMessageProperties().setMessageId(messageId.toString());
                        message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        message.getMessageProperties().setHeader("X-Event-Type", notificationType);
                        message.getMessageProperties().setTimestamp(new Date());
                        return message;
                    },
                    new CorrelationData(messageId.toString())
            );

            log.debug("Published message ID: {} to exchange: {} with routing key: {}",
                    messageId, notificationExchange, routingKey);
        } catch (Exception e) {
            log.error("Failed to publish message ID: {} - Error: {}", messageId, e.getMessage(), e);
            throw new AmqpException("Failed to publish event message", e);
        }
    }
}
