package sgw.common.infra.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sgw.common.infra.condition.ConditionalOnPropertySimple;
import sgw.common.infra.messaging.RabbitMessagingConstants;

@Configuration
@ConditionalOnPropertySimple(prefix = "app.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfiguration {

    @Value("${app.rabbit.host:localhost}")
    private String host;

    @Value("${app.rabbit.port:5672}")
    private int port;

    @Value("${app.rabbit.username:guest}")
    private String username;

    @Value("${app.rabbit.password:guest}")
    private String password;

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    @Bean
    public TopicExchange batchPingExchange() {
        return new TopicExchange(RabbitMessagingConstants.BATCH_PING_EXCHANGE);
    }

    @Bean
    public Queue batchPingQueue() {
        return new Queue(RabbitMessagingConstants.BATCH_PING_QUEUE, true);
    }

    @Bean
    public Binding batchPingBinding() {
        return BindingBuilder.bind(batchPingQueue())
            .to(batchPingExchange())
            .with(RabbitMessagingConstants.BATCH_PING_ROUTING_KEY);
    }

    @Bean
    public TopicExchange workerCommandExchange() {
        return new TopicExchange(RabbitMessagingConstants.WORKER_COMMAND_EXCHANGE);
    }

    @Bean
    public Queue workerCommandQueue() {
        return new Queue(RabbitMessagingConstants.WORKER_COMMAND_QUEUE, true);
    }

    @Bean
    public Binding workerCommandBinding() {
        return BindingBuilder.bind(workerCommandQueue())
            .to(workerCommandExchange())
            .with(RabbitMessagingConstants.WORKER_COMMAND_ROUTING_KEY);
    }
}

