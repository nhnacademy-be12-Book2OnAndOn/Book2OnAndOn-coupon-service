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
    public static final String ROUTING_KEY_WELCOME = "coupon.welcome";
    public static final String DLX_ROUTING_KEY_WELCOME = "coupon.welcome.dlq";
    public static final String QUEUE_WELCOME_DLQ = "book2.dev.welcome.dlq";

    public static final String QUEUE_BIRTHDAY = "book2.dev.birthday.queue";
    public static final String QUEUE_BIRTHDAY_DLQ = "book2.dev.birthday.dlq";
    public static final String ROUTING_KEY_BIRTHDAY = "coupon.birthday";
    public static final String DLX_ROUTING_KEY_BIRTHDAY = "coupon.birthday.dlq";

    public static final String DLX_EXCHANGE = "book2.dev.dlx.coupon.exchange";

    //공통 exchange
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    //WelcomeCoupon
    @Bean
    public Queue welcomeQueue() {
        return QueueBuilder.durable(QUEUE_WELCOME)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY_WELCOME)
                .build();
    }

    @Bean
    public Binding welcomeBinding() {
        return BindingBuilder.bind(welcomeQueue())
                .to(exchange())
                .with(ROUTING_KEY_WELCOME);

    }

    @Bean
    public Queue welcomeDlq() {
        return new Queue(QUEUE_WELCOME_DLQ, true);
    }

    @Bean
    public Binding welcomeDlqBinding() {
        return BindingBuilder.bind(welcomeDlq())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY_WELCOME);
    }

    //BirthdayCoupon
    @Bean
    public Queue birthdayQueue() {
        return QueueBuilder.durable(QUEUE_BIRTHDAY)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY_BIRTHDAY)
                .build();
    }

    @Bean
    public Binding birthdayBinding() {
        return BindingBuilder.bind(birthdayQueue())
                .to(exchange())
                .with(ROUTING_KEY_BIRTHDAY);
    }

    @Bean
    public Queue birthdayDlq() {
        return new Queue(QUEUE_BIRTHDAY_DLQ, true);
    }

    @Bean
    public Binding birthdayDlqBinding() {
        return BindingBuilder.bind(birthdayDlq())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY_BIRTHDAY);
    }
}
