package com.cas.common.infra.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Kafka Producer 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 메시지 전송
     */
    public void send(String topic, String message) {
        send(topic, null, message);
    }

    /**
     * 메시지 전송 (Key 포함)
     * 
     * Key를 지정하면 같은 Key를 가진 메시지는 같은 파티션으로 전송됩니다.
     * userId를 Key로 사용하면 같은 사용자의 작업은 순서대로 처리됩니다.
     * 
     * @param topic Kafka 토픽
     * @param key 파티션 키 (userId 권장)
     * @param message 메시지 내용
     */
    public void send(String topic, String key, String message) {
        ListenableFuture<SendResult<String, String>> future = 
            kafkaTemplate.send(topic, key, message);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("Sent message=[{}] with key=[{}] to topic=[{}] with offset=[{}]",
                        message, key, topic, result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("Failed to send message=[{}] with key=[{}] to topic=[{}]",
                        message, key, topic, ex);
            }
        });
    }
}

