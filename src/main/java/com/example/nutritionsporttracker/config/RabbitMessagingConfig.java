
package com.example.nutritionsporttracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

    public static final String EXCHANGE = "nutrise.ai.exchange";

    public static final String REQUEST_QUEUE =
            "nutrise.ai.report.request.queue";

    public static final String RESULT_QUEUE =
            "nutrise.ai.report.result.queue";

    public static final String REQUEST_ROUTING_KEY =
            "ai.report.requested";

    public static final String RESULT_ROUTING_KEY =
            "ai.report.generated";

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue reportRequestQueue() {
        return QueueBuilder.durable(REQUEST_QUEUE).build();
    }

    @Bean
    public Queue reportResultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE).build();
    }

    @Bean
    public Binding requestBinding(
            @Qualifier("reportRequestQueue") Queue queue,
            DirectExchange aiExchange
    ) {
        return BindingBuilder.bind(queue)
                .to(aiExchange)
                .with(REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding resultBinding(
            @Qualifier("reportResultQueue") Queue queue,
            DirectExchange aiExchange
    ) {
        return BindingBuilder.bind(queue)
                .to(aiExchange)
                .with(RESULT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitJsonMessageConverter(
            ObjectMapper objectMapper
    ) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}