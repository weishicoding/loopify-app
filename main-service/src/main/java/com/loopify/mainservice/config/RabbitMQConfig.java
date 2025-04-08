package com.loopify.mainservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.notifications}")
    private String notificationExchange;

    @Value("${rabbitmq.queue.follow-notifications}")
    private String followNotificationQueue;

    @Value("${rabbitmq.queue.comment-notifications}")
    private String commentNotificationQueue;

    @Value("${rabbitmq.routing-key.follow}")
    private String followRoutingKey;

    @Value("${rabbitmq.routing-key.comment}")
    private String commentRoutingKey;

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(notificationExchange);
    }

    @Bean
    public Queue followNotificationQueue() {
        return new Queue(followNotificationQueue);
    }

    @Bean
    public Queue commentNotificationQueue() {
        return new Queue(commentNotificationQueue);
    }

    @Bean
    public Binding followNotificationBinding() {
        return BindingBuilder
                .bind(followNotificationQueue())
                .to(notificationExchange())
                .with(followRoutingKey);
    }

    @Bean
    public Binding commentNotificationBinding() {
        return BindingBuilder
                .bind(commentNotificationQueue())
                .to(notificationExchange())
                .with(commentRoutingKey);
    }

    // Other beans remain the same, it likes activities or products
}
