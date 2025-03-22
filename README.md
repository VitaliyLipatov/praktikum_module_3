# praktikum_module_3
Итоговый проект третьего модуля Apache Kafka (Яндекс Практикум).

Задание 1.
Инфраструктура проекта для запуска в ресурсах в папке infra-jdbc-connector.
Запускается стандартно командой 'docker-compose up -d' через консоль.

Таблица с экспериментами
Эксперимент			batch.size			batch.max.rows			linger.ms		compression.type		buffer.memory		source record write rate(kops/sec)
1					100					1						1000			none					33444432			25.5
2					126000				1000					2000			none					33444432			154
3					126000				1000					2000			snappy					33444432			158
4					630000				5000					50			    none					16722216			148
5					630000				5000					1000			none					33444432			163
6					630000				5000					500				gzip					33444432			163
7					882000				7000					0				none					50166648			132

Согласно данным из графаны средний объём одной записи составляет 126 байт.
Исходя из него можно рассчитать показатель batch.size = 126 * batch.max.rows.
В первом эксперименте batch.size был подобран некорректно, поэтому скорость записи составила всего 25.5 kops/sec.
Во втором эксприменте batch.size был рассчитан по формуле 1000 * 126 = 126 000, соответственно скорость возросла до 154 kops/sec.
В третьем эксперименте при прочих одинаковых показателях был добавлен алгоритм сжатия compression.type, 
что позволило увеличить скорость записи до 158 kops/sec.
В последующих экспериментах изменялся показатель linger.ms, который отвечает за время ожидания дополнительных сообщений 
перед отправкой текущего пакета. Если данный показатель сделать слишком небольшим, то мы можем отдавать пачки слишком 
рано, до того как они полностью заполнены/сформированы. Если же наоборот сделать его слишком большим, то мы можем "простаивать"
и отдавать пакты слишком медленно. Показатель должен коррелировать с кол-ом записей, что видно из эксприментов 4-7.
Для 5000 записей оптимальным оказалось значение в 1000 мс, при этом если использовать алгоритм сжатия gzip, то при 
показатели linger.ms = 500 мс можно достичь той же скорости. При большом кол-ве записей и значении по умолчанию
linger.ms = 0 мс видим, что скорость записи будет ниже.
Параметр buffer.memory определяет общее количество байт памяти, которое можно использовать для буферизации записей, 
ожидающих отправки на сервер. В моём случае изменение параметра не влияло на результаты экспериментов, 
выделяемой памяти хватало для отправки пакетов.

Сконфигурировать коннектор можно с помощью запроса ниже, меняя при необходимости параметры

curl -X PUT \
-H "Content-Type: application/json" \
--data '{
"connector.class":"io.confluent.connect.jdbc.JdbcSourceConnector",
"tasks.max":"1",
"connection.url":"jdbc:postgresql://postgres:5432/customers?user=postgres-user&password=postgres-pw&useSSL=false",
"connection.attempts":"5",
"connection.backoff.ms":"50000",
"mode":"timestamp",
"timestamp.column.name":"updated_at",
"topic.prefix":"postgresql-jdbc-bulk-",
"table.whitelist": "users",
"poll.interval.ms": "200",
"batch.max.rows": 7000,
"producer.override.linger.ms": 0,
"producer.override.batch.size": 882000,
"compression.type": "snappy",
"transforms":"MaskField",
"transforms.MaskField.type":"org.apache.kafka.connect.transforms.MaskField$Value",
"transforms.MaskField.fields":"private_info",
"transforms.MaskField.replacement":"CENSORED"
}' \
http://localhost:8083/connectors/postgres-source/config

Задание 2.
Инфраструктура проекта для запуска в ресурсах в папке infra-prometheus.
Конфигурация коннектора описана в папке prometheus в файле config.json.
Там указан топик test-topic, куда необходимо отправлять сообщения и url, по которому будут отдаваться метрики
http://localhost:8080/metrics, также указан класс-коннектор PrometheusSinkConnector.
Поднимается инфраструктура стандартно командой 'docker-compose up -d' через консоль.
Для конфигурации коннектора используется команда:
'curl -X POST -H "Content-Type: application/json" --data @config.json http://localhost:8083/connectors'
Для получения статуса коннектора:
'curl http://localhost:8083/connectors/prometheus-connector/status'
Если вдруг требуется удалить коннектор:
'curl -X DELETE http://localhost:8083/connectors/prometheus-connector'

Проект собирается с помощью maven, джарник praktikum_module_3-1.0.jar необходимо положить в директорию 
confluent-hub-components.
Класс PrometheusHttpServer - предоставляет endpoint /merics, который будет опрашивать прометеус
Класс PrometheusSinkTask - реализует логику преобразования сообщения из топика test-topic в строковый формат, 
понятный для прометеуса (ключевой метод public void put(Collection<SinkRecord> collection) {})
Класс PrometheusSinkConnector - это основной класс коннектора, инициализирует коннектор и конфигурацию, 
а также  определяет, сколько задач будет запущено (tasks.max) и какие параметры будут переданы каждой задаче.

После запуск коннектора из консоли нужно перейти в kafka ui по адресу http://localhost:8085 
и отправить сообщение с метрикой в топик test-topic:
{
"name": "Alloc",
"type": "gauge",
"description": "Alloc is bytes of allocated heap objects.",
"value": 2.4293912E7
}
Далее в браузере перейдём по адресу http://localhost:8080/metrics и увидим отражение метрики:
# Base URL: http://localhost
# HELP Alloc Alloc is bytes of allocated heap objects.
# TYPE Alloc gauge
Alloc 24293912.000000

Аналогичным образом можно отправить ещё несколько сообщений в топик.
Прикладываю также скриншот для подтверждения в ресурсы.


