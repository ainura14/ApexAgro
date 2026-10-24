package com.apexagro.common.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.apexagro.common.logging.CorrelationId;

import tools.jackson.databind.json.JsonMapper;

/** BE3-09: exchanges, JSON-сообщения, проброс correlation id через RabbitMQ. */
@Configuration(proxyBeanMethods = false)
class MessagingConfig {

    @Bean
    Declarables apexagroExchanges() {
        DirectExchange commands = ExchangeBuilder.directExchange(Messaging.COMMANDS_EXCHANGE).durable(true).build();
        DirectExchange dlx = ExchangeBuilder.directExchange(Messaging.DEAD_LETTER_EXCHANGE).durable(true).build();
        TopicExchange realtime = ExchangeBuilder.topicExchange(Messaging.REALTIME_EXCHANGE).durable(true).build();
        return new Declarables(commands, dlx, realtime);
    }

    /** JSON в теле сообщений; классы для десериализации — только из пакетов приложения. */
    @Bean
    MessageConverter amqpMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper, "com.apexagro");
    }

    /** Исходящие сообщения получают заголовок X-Correlation-Id из MDC. */
    @Bean
    RabbitTemplateCustomizer correlationIdRabbitTemplateCustomizer() {
        return template -> template.addBeforePublishPostProcessors(message -> {
            CorrelationId.current().ifPresent(id -> {
                if (message.getMessageProperties().getHeader(CorrelationId.HEADER) == null) {
                    message.getMessageProperties().setHeader(CorrelationId.HEADER, id);
                }
            });
            return message;
        });
    }

    /**
     * Фабрика слушателей Boot (ретраи 3× → DLQ из application.yml) + совет, который кладёт
     * correlation id входящего сообщения в MDC на время обработки.
     */
    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        List<Advice> chain = new ArrayList<>();
        chain.add(correlationIdAdvice());
        if (factory.getAdviceChain() != null) {
            chain.addAll(Arrays.asList(factory.getAdviceChain()));
        }
        factory.setAdviceChain(chain.toArray(Advice[]::new));
        return factory;
    }

    static MethodInterceptor correlationIdAdvice() {
        return invocation -> {
            String correlationId = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Message message) {
                    Object header = message.getMessageProperties().getHeader(CorrelationId.HEADER);
                    correlationId = header == null ? null : CorrelationId.sanitizeOrGenerate(header.toString());
                }
            }
            Object[] result = new Object[1];
            Throwable[] error = new Throwable[1];
            CorrelationId.runWith(correlationId != null ? correlationId : CorrelationId.generate(), () -> {
                try {
                    result[0] = invocation.proceed();
                } catch (Throwable t) {
                    error[0] = t;
                }
            });
            if (error[0] != null) {
                throw error[0];
            }
            return result[0];
        };
    }
}
