package sgw.common.infra.messaging;

public final class RabbitMessagingConstants {

    private RabbitMessagingConstants() {
    }

    public static final String BATCH_PING_EXCHANGE = "batch.ping.exchange";
    public static final String BATCH_PING_QUEUE = "batch.ping.queue";
    public static final String BATCH_PING_ROUTING_KEY = "batch.ping.request";

    public static final String WORKER_COMMAND_EXCHANGE = "worker.command.exchange";
    public static final String WORKER_COMMAND_QUEUE = "worker.command.queue";
    public static final String WORKER_COMMAND_ROUTING_KEY = "worker.command.ping";
}

