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

