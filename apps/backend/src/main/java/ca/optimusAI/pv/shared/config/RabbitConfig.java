package ca.optimusAI.pv.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // ─── Exchanges ────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange validationEvents() {
        return new TopicExchange("validation.events", true, false);
    }

    @Bean
    public DirectExchange reportsExchange() {
        return new DirectExchange("reports.generate", true, false);
    }

    // ─── Queues ───────────────────────────────────────────────────────────────

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable("validation.notifications")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "validation.notifications.dlq")
                .build();
    }

    @Bean
    public Queue notificationsDlq() {
        return QueueBuilder.durable("validation.notifications.dlq").build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable("audit.all").build();
    }

    @Bean
    public Queue reportsQueue() {
        return QueueBuilder.durable("reports.generate").build();
    }

    // ─── Bindings ─────────────────────────────────────────────────────────────

    @Bean
    public Binding notifBinding(Queue notificationsQueue, TopicExchange validationEvents) {
        return BindingBuilder.bind(notificationsQueue)
                .to(validationEvents)
                .with("session.*");
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange validationEvents) {
        return BindingBuilder.bind(auditQueue)
                .to(validationEvents)
                .with("#");
    }

    @Bean
    public Binding reportsBinding(Queue reportsQueue, DirectExchange reportsExchange) {
        return BindingBuilder.bind(reportsQueue)
                .to(reportsExchange)
                .with("reports.generate");
    }

    // ─── Message Converter ────────────────────────────────────────────────────

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
