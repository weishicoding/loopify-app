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

    @Value("${rabbitmq.routing-key.follow}")
    private String followRoutingKey;

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(notificationExchange);
    }

    @Bean
    public Queue followNotificationQueue() {
        return new Queue(followNotificationQueue);
    }

    @Bean
    public Binding followNotificationBinding() {
        return BindingBuilder
                .bind(followNotificationQueue())
                .to(notificationExchange())
                .with(followRoutingKey);
    }

    // Other beans remain the same, it likes activities or products
}
