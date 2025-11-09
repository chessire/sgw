package sgw.app.worker.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import sgw.common.infra.messaging.RabbitMessagingConstants;

@Component
public class WorkerCommandListener {

    private static final Logger log = LoggerFactory.getLogger(WorkerCommandListener.class);

    @RabbitListener(queues = RabbitMessagingConstants.WORKER_COMMAND_QUEUE)
    public String processCommand(String payload) {
        log.info("Worker received command: {}", payload);
        if ("PING".equalsIgnoreCase(payload)) {
            return "PONG";
        }
        return "UNKNOWN";
    }
}

