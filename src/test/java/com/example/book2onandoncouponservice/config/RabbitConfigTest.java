package com.example.book2onandoncouponservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

class RabbitConfigTest {

    private final RabbitConfig rabbitConfig = new RabbitConfig();

    @Test
    @DisplayName("MessageConverter Bean 생성 확인")
    void jsonMessageConverter() {
        MessageConverter converter = rabbitConfig.jsonMessageConverter();
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    @DisplayName("Exchanges 생성 확인")
    void exchanges() {
        assertThat(rabbitConfig.userExchange()).isInstanceOf(DirectExchange.class);
        assertThat(rabbitConfig.userExchange().getName()).isEqualTo(RabbitConfig.USER_EXCHANGE);

        assertThat(rabbitConfig.orderExchange()).isInstanceOf(DirectExchange.class);
        assertThat(rabbitConfig.orderExchange().getName()).isEqualTo(RabbitConfig.ORDER_EXCHANGE);

        assertThat(rabbitConfig.couponExchange()).isInstanceOf(DirectExchange.class);
        assertThat(rabbitConfig.couponExchange().getName()).isEqualTo(RabbitConfig.COUPON_EXCHANGE);

        assertThat(rabbitConfig.dlxExchange()).isInstanceOf(DirectExchange.class);
        assertThat(rabbitConfig.dlxExchange().getName()).isEqualTo(RabbitConfig.DLX_EXCHANGE);
    }

    @Test
    @DisplayName("Welcome Queue 및 Binding 설정 확인 (DLX 포함)")
    void welcomeConfiguration() {
        // Queue
        Queue queue = rabbitConfig.welcomeQueue();
        assertThat(queue.getName()).isEqualTo(RabbitConfig.QUEUE_WELCOME);

        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfig.DLX_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitConfig.DLX_ROUTING_KEY_WELCOME);

        // Binding
        Binding binding = rabbitConfig.welcomeBinding();
        assertThat(binding.getDestination()).isEqualTo(RabbitConfig.QUEUE_WELCOME);
        assertThat(binding.getExchange()).isEqualTo(RabbitConfig.USER_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitConfig.ROUTING_KEY_WELCOME);

        // DLQ
        Queue dlq = rabbitConfig.welcomeDlq();
        assertThat(dlq.getName()).isEqualTo(RabbitConfig.QUEUE_WELCOME_DLQ);

        // DLQ Binding
        Binding dlqBinding = rabbitConfig.welcomeDlqBinding();
        assertThat(dlqBinding.getDestination()).isEqualTo(RabbitConfig.QUEUE_WELCOME_DLQ);
        assertThat(dlqBinding.getExchange()).isEqualTo(RabbitConfig.DLX_EXCHANGE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(RabbitConfig.DLX_ROUTING_KEY_WELCOME);
    }

    @Test
    @DisplayName("Birthday Queue 및 Binding 설정 확인")
    void birthdayConfiguration() {
        // Queue
        Queue queue = rabbitConfig.birthdayQueue();
        assertThat(queue.getName()).isEqualTo(RabbitConfig.QUEUE_BIRTHDAY);
        
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfig.DLX_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitConfig.DLX_ROUTING_KEY_BIRTHDAY);

        // Binding
        Binding binding = rabbitConfig.birthdayBinding();
        assertThat(binding.getDestination()).isEqualTo(RabbitConfig.QUEUE_BIRTHDAY);
        assertThat(binding.getExchange()).isEqualTo(RabbitConfig.USER_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitConfig.ROUTING_KEY_BIRTHDAY);

        // DLQ
        Queue dlq = rabbitConfig.birthdayDlq();
        assertThat(dlq.getName()).isEqualTo(RabbitConfig.QUEUE_BIRTHDAY_DLQ);

        // DLQ Binding
        Binding dlqBinding = rabbitConfig.birthdayDlqBinding();
        assertThat(dlqBinding.getDestination()).isEqualTo(RabbitConfig.QUEUE_BIRTHDAY_DLQ);
        assertThat(dlqBinding.getExchange()).isEqualTo(RabbitConfig.DLX_EXCHANGE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(RabbitConfig.DLX_ROUTING_KEY_BIRTHDAY);
    }

    @Test
    @DisplayName("Cancel Queue 및 Binding 설정 확인")
    void cancelConfiguration() {
        // Queue
        Queue queue = rabbitConfig.cancelQueue();
        assertThat(queue.getName()).isEqualTo(RabbitConfig.QUEUE_CANCEL);

        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfig.DLX_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitConfig.DLX_ROUTING_KEY_CANCEL);

        // Binding
        Binding binding = rabbitConfig.couponCancelBinding();
        assertThat(binding.getDestination()).isEqualTo(RabbitConfig.QUEUE_CANCEL);
        assertThat(binding.getExchange()).isEqualTo(RabbitConfig.ORDER_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitConfig.ROUTING_KEY_CANCEL);

        // DLQ
        Queue dlq = rabbitConfig.cancelDlq();
        assertThat(dlq.getName()).isEqualTo(RabbitConfig.QUEUE_CANCEL_DLQ);

        // DLQ Binding
        Binding dlqBinding = rabbitConfig.cancelDlqBinding();
        assertThat(dlqBinding.getDestination()).isEqualTo(RabbitConfig.QUEUE_CANCEL_DLQ);
        assertThat(dlqBinding.getExchange()).isEqualTo(RabbitConfig.DLX_EXCHANGE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(RabbitConfig.DLX_ROUTING_KEY_CANCEL);
    }

    @Test
    @DisplayName("Issue Queue 및 Binding 설정 확인")
    void issueConfiguration() {
        // Queue
        Queue queue = rabbitConfig.issueQueue();
        assertThat(queue.getName()).isEqualTo(RabbitConfig.QUEUE_ISSUE);

        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfig.DLX_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitConfig.DLX_ROUTING_KEY_ISSUE);

        // Binding
        Binding binding = rabbitConfig.couponIssueBinding();
        assertThat(binding.getDestination()).isEqualTo(RabbitConfig.QUEUE_ISSUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitConfig.COUPON_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitConfig.ROUTING_KEY_ISSUE);

        // DLQ
        Queue dlq = rabbitConfig.issueDlq();
        assertThat(dlq.getName()).isEqualTo(RabbitConfig.QUEUE_ISSUE_DLQ);

        // DLQ Binding
        Binding dlqBinding = rabbitConfig.issueDlqBinding();
        assertThat(dlqBinding.getDestination()).isEqualTo(RabbitConfig.QUEUE_ISSUE_DLQ);
        assertThat(dlqBinding.getExchange()).isEqualTo(RabbitConfig.DLX_EXCHANGE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(RabbitConfig.DLX_ROUTING_KEY_ISSUE);
    }
}