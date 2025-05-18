package com.loopify.mainservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.follow-notifications}")
    private String followTopic;

    @Value("${app.kafka.topics.comment-notifications}")
    private String commentTopic;

    @Bean
    public NewTopic followNotificationsTopic() {
        return TopicBuilder.name(followTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic commentNotificationsTopic() {
        return TopicBuilder.name(commentTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
