package jo.jung.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collections;

public class KafkaConsumerFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> Flux<Tuple2<String, T>> createJsonConsumer(String topic, String groupId, Class<T> clazz) {
        ReceiverOptions<String, String> options = ReceiverOptions
                .<String, String>create(KafkaConfigUtils.defaultConsumerProps(groupId))
                .subscription(Collections.singletonList(topic));

        return KafkaReceiver.create(options)
                .receive()
                .doOnNext(record -> record.receiverOffset().acknowledge())
                .flatMap(record -> {
                    try {
                        T obj = objectMapper.readValue(record.value(), clazz);
                        String key = record.key();
                        return Flux.just(Tuples.of(key, obj));
                    } catch (Exception e) {
                        return Flux.error(new RuntimeException("Kafka JSON deserialization error", e));
                    }
                });
    }
}
