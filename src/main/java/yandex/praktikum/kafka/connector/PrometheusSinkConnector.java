package yandex.praktikum.kafka.connector;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PrometheusSinkConnector extends SinkConnector {

    private String prometheusUrl;
    private int prometheusPort;

    @Override
    public void start(Map<String, String> props) {
        try {
            log.info("Starting Prometheus Sink Connector with props {}", props);
            this.prometheusUrl = "http://localhost";
            this.prometheusPort = 8080;
            PrometheusHttpServer.getInstance(prometheusUrl, prometheusPort);
        } catch (Exception ex) {
            log.error("Error starting Prometheus Sink Connector", ex);
        }
    }

    @Override
    public Class<? extends Task> taskClass() {
        return PrometheusSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        Map<String, String> taskProps = new HashMap<>();
        taskProps.put("url", prometheusUrl);
        taskProps.put("port", String.valueOf(prometheusPort));
        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(taskProps);
        }
        return taskConfigs;
    }

    @Override
    public void stop() {

    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
                .define("prometheus.listener.url", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "url prometheus");
    }

    @Override
    public String version() {
        return "1.0.0";
    }
}
