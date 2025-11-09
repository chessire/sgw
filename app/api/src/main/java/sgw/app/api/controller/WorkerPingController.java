package sgw.app.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import sgw.common.infra.messaging.RabbitMessagingConstants;

@Controller
@RequestMapping("/api/worker")
public class WorkerPingController {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/ping")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pingWorker() {
        if (rabbitTemplate == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "ERROR");
            body.put("message", "RabbitMQ is not configured");
            return ResponseEntity.status(503).body(body);
        }

        Object response = rabbitTemplate.convertSendAndReceive(
            RabbitMessagingConstants.WORKER_COMMAND_EXCHANGE,
            RabbitMessagingConstants.WORKER_COMMAND_ROUTING_KEY,
            "PING");

        String payload = response != null ? response.toString() : "NO_RESPONSE";
        Map<String, Object> body = new HashMap<>();
        body.put("status", "OK");
        body.put("data", payload);
        return ResponseEntity.ok(body);
    }
}

