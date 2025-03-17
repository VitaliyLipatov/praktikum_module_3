package yandex.praktikum.kafka.connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.jetty.util.ajax.JSON;
import yandex.praktikum.kafka.connector.model.MetricEvent;

import java.util.Properties;

@Slf4j
public class Producer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void send() {
        // Конфигурация продюсера – адрес сервера, сериализаторы для ключа и значения.
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094, localhost:9095, localhost:9096");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Синхронная репликация - требует, чтобы все реплики синхронно подтвердили получение сообщения,
        // только после этого оно считается успешно отправленным
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        // Количество повторных попыток при отправке сообщений, если возникает ошибка.
        // Если три раза произошли ошибки, то сообщение считается неотправленным и ошибка будет возвращена продюсеру.
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
        // Минимум 2 реплики должны подтвердить запись
        properties.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2");

        // Создание продюсера
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        MetricEvent metricEvent = MetricEvent.builder()
                .name("Alloc")
                .type("gauge")
                .value(24293912)
                .description("Alloc is bytes of allocated heap objects.")
                .build();

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>("test-topic", metricEvent.getName(),
                    MAPPER.writeValueAsString(metricEvent));
            producer.send(record);
            log.info("Sent record: {}", record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Закрытие продюсера
        producer.close();
    }
}
