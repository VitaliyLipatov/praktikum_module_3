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
    private String prometheusPort;

    @Override
    public void start(Map<String, String> props) {
        try {
            log.info("Starting Prometheus Sink Connector with props {}", props);
            prometheusUrl = props.get("prometheusUrl");
            prometheusPort = props.get("prometheusPort");
            PrometheusHttpServer.getInstance(prometheusUrl, Integer.parseInt(prometheusPort));
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
        taskProps.put("prometheusUrl", prometheusUrl);
        taskProps.put("prometheusPort", prometheusPort);
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
                .define("prometheusUrl", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "url prometheus")
                .define("prometheusPort", ConfigDef.Type.INT, 8080, ConfigDef.Importance.HIGH, "port prometheus");
    }

    @Override
    public String version() {
        return "1.0.0";
    }
}
