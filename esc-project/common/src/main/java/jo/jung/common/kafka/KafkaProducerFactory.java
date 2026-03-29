package jo.jung.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

public class KafkaProducerFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final KafkaSender<String, String> kafkaSender =
            KafkaSender.create(SenderOptions.create(KafkaConfigUtils.defaultProducerProps()));

    public static <T> Mono<Void> sendJson(String topic, String key, T data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);
            SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
            return kafkaSender.send(Mono.just(senderRecord)).then();
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Kafka JSON serialization error", e));
        }
    }
}