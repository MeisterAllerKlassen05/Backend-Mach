package com.example.rideofferservice.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue.name}")
    private String queueName;

    @Value("${app.rabbitmq.routing.key}")
    private String routingKey;

    /**
     * Erstelle den Topic-Exchange für Ride-Offer-Events.
     */
    @Bean
    public TopicExchange rideOfferExchange() {
        return new TopicExchange(exchangeName);
    }

    /**
     * Erstelle die Queue für Ride-Offer-Events.
     */
    @Bean
    public Queue rideOfferQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    /**
     * Binde die Queue an den Exchange mit dem Routing-Key.
     */
    @Bean
    public Binding rideOfferBinding(Queue rideOfferQueue, TopicExchange rideOfferExchange) {
        return BindingBuilder.bind(rideOfferQueue)
                .to(rideOfferExchange)
                .with(routingKey);
    }
}
