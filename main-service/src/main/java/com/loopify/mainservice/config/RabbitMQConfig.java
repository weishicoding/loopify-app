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

    @Value("${rabbitmq.queue.order-notifications}")
    private String orderNotificationQueue;

    @Value("${rabbitmq.queue.activity-notifications}")
    private String activityNotificationQueue;

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
    public Queue orderNotificationQueue() {
        return new Queue(orderNotificationQueue);
    }

    @Bean
    public Queue activityNotificationQueue() {
        return new Queue(activityNotificationQueue);
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
