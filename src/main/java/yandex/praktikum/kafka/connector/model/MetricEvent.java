package yandex.praktikum.kafka.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricEvent {
    private String name;
    private String type;
    private String description;
    private double value;

}
