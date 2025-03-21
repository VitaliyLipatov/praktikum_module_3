package yandex.praktikum.kafka.connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import yandex.praktikum.kafka.connector.model.MetricEvent;

import java.util.Collection;
import java.util.Map;

@Slf4j
public class PrometheusSinkTask extends SinkTask {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private PrometheusHttpServer httpServer;

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void start(Map<String, String> map) {
        try {
            httpServer = PrometheusHttpServer.getInstance("http://localhost", 9090);
        } catch (Exception ex) {
            log.error("Ошибка при инициализации PrometheusHttpServer", ex);
        }
    }

    @Override
    public void put(Collection<SinkRecord> collection) {
        collection.forEach(record -> {
            String stringRecord = (String) record.value();
            try {
                MetricEvent metricEvent = MAPPER.readValue(stringRecord, MetricEvent.class);
                String prometheusData = String.format(
                        "# HELP %s %s\n# TYPE %s %s\n%s %f\n",
                        metricEvent.getName(), metricEvent.getDescription(), metricEvent.getName(),
                        metricEvent.getType(), metricEvent.getName(), metricEvent.getValue());
                httpServer.addMetric(metricEvent.getName(), prometheusData);
            } catch (JsonProcessingException ex) {
                log.error("Ошибка при парсинге сообщения из kafka", ex);
            }
        });
    }

    @Override
    public void stop() {

    }
}
