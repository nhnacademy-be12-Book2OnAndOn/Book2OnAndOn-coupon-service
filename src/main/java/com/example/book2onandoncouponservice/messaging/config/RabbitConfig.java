package com.example.book2onandoncouponservice.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "book2.dev.user.exchange";
    public static final String QUEUE_WELCOME = "book2.dev.welcome.queue";
    public static final String ROUTING_KEY = "coupon.welcome";
    public static final String DLX_EXCHANGE = "book2.dev.dlx.exchange";
    public static final String QUEUE_WELCOME_DLQ = "book2.dev.welcome.dlq";
    public static final String DLX_ROUTING_KEY = "coupon.welcome.dlq";

    @Bean
    public Queue welcomeQueue() {

        return QueueBuilder.durable(QUEUE_WELCOME)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(welcomeQueue())
                .to(exchange())
                .with(ROUTING_KEY);

    }

    @Bean
    public Queue welcomeDlq() {
        return new Queue(QUEUE_WELCOME_DLQ, true);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(welcomeDlq())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }
}
