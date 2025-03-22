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

Задание 3.
Лог запуска Debezium PostgresConnector из докера:

2025-03-20 21:11:11 [2025-03-20 16:11:11,375] INFO Starting PostgresConnectorTask with configuration:
2025-03-20 21:11:11    connector.class = io.debezium.connector.postgresql.PostgresConnector
2025-03-20 21:11:11    database.user = postgres-user
2025-03-20 21:11:11    database.dbname = customers
2025-03-20 21:11:11    transforms.unwrap.delete.handling.mode = rewrite
2025-03-20 21:11:11    topic.creation.default.partitions = -1
2025-03-20 21:11:11    transforms = unwrap
2025-03-20 21:11:11    database.server.name = customers
2025-03-20 21:11:11    database.port = 5432
2025-03-20 21:11:11    table.whitelist = public.customers
2025-03-20 21:11:11    topic.creation.enable = true
2025-03-20 21:11:11    topic.prefix = customers
2025-03-20 21:11:11    task.class = io.debezium.connector.postgresql.PostgresConnectorTask
2025-03-20 21:11:11    database.hostname = postgres
2025-03-20 21:11:11    database.password = ********
2025-03-20 21:11:11    transforms.unwrap.drop.tombstones = false
2025-03-20 21:11:11    topic.creation.default.replication.factor = -1
2025-03-20 21:11:11    name = pg-connector
2025-03-20 21:11:11    transforms.unwrap.type = io.debezium.transforms.ExtractNewRecordState
2025-03-20 21:11:11    skipped.operations = none
2025-03-20 21:11:11  (io.debezium.connector.common.BaseSourceTask)
2025-03-20 21:11:11 [2025-03-20 16:11:11,376] INFO Loading the custom source info struct maker plugin: io.debezium.connector.postgresql.PostgresSourceInfoStructMaker (io.debezium.config.CommonConnectorConfig)
2025-03-20 21:11:11 [2025-03-20 16:11:11,378] INFO Loading the custom topic naming strategy plugin: io.debezium.schema.SchemaTopicNamingStrategy (io.debezium.config.CommonConnectorConfig)
2025-03-20 21:11:11 [2025-03-20 16:11:11,388] INFO Connection gracefully closed (io.debezium.jdbc.JdbcConnection)
2025-03-20 21:11:11 [2025-03-20 16:11:11,431] WARN Type [oid:13529, name:_pg_user_mappings] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,431] WARN Type [oid:13203, name:cardinal_number] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,431] WARN Type [oid:13206, name:character_data] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,431] WARN Type [oid:13208, name:sql_identifier] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,431] WARN Type [oid:13214, name:time_stamp] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,431] WARN Type [oid:13216, name:yes_or_no] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,475] INFO No previous offsets found (io.debezium.connector.common.BaseSourceTask)
2025-03-20 21:11:11 [2025-03-20 16:11:11,495] WARN Type [oid:13529, name:_pg_user_mappings] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,495] WARN Type [oid:13203, name:cardinal_number] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,495] WARN Type [oid:13206, name:character_data] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,495] WARN Type [oid:13208, name:sql_identifier] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,495] WARN Type [oid:13214, name:time_stamp] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,495] WARN Type [oid:13216, name:yes_or_no] is already mapped (io.debezium.connector.postgresql.TypeRegistry)
2025-03-20 21:11:11 [2025-03-20 16:11:11,522] INFO Connector started for the first time. (io.debezium.connector.common.BaseSourceTask)
2025-03-20 21:11:11 [2025-03-20 16:11:11,523] INFO No previous offset found (io.debezium.connector.postgresql.PostgresConnectorTask)
2025-03-20 21:11:11 [2025-03-20 16:11:11,530] INFO user 'postgres-user' connected to database 'customers' on PostgreSQL 16.4 (Debian 16.4-1.pgdg110+2) on x86_64-pc-linux-gnu, compiled by gcc (Debian 10.2.1-6) 10.2.1 20210110, 64-bit with roles:
2025-03-20 21:11:11 role 'pg_read_all_settings' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_database_owner' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_stat_scan_tables' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_checkpoint' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_write_server_files' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_use_reserved_connections' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'postgres-user' [superuser: true, replication: true, inherit: true, create role: true, create db: true, can log in: true]
2025-03-20 21:11:11 role 'pg_read_all_data' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_write_all_data' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_monitor' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_read_server_files' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_create_subscription' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_execute_server_program' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_read_all_stats' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false]
2025-03-20 21:11:11 role 'pg_signal_backend' [superuser: false, replication: false, inherit: true, create role: false, create db: false, can log in: false] (io.debezium.connector.postgresql.PostgresConnectorTask)
2025-03-20 21:11:11 [2025-03-20 16:11:11,538] INFO Obtained valid replication slot ReplicationSlot [active=false, latestFlushedLsn=null, catalogXmin=null] (io.debezium.connector.postgresql.connection.PostgresConnection)
2025-03-20 21:11:11 [2025-03-20 16:11:11,556] INFO Creating replication slot with command CREATE_REPLICATION_SLOT "debezium"  LOGICAL decoderbufs  (io.debezium.connector.postgresql.connection.PostgresReplicationConnection)
2025-03-20 21:11:11 [2025-03-20 16:11:11,591] INFO Requested thread factory for component PostgresConnector, id = customers named = SignalProcessor (io.debezium.util.Threads)
2025-03-20 21:11:11 [2025-03-20 16:11:11,616] INFO Requested thread factory for component PostgresConnector, id = customers named = change-event-source-coordinator (io.debezium.util.Threads)
2025-03-20 21:11:11 [2025-03-20 16:11:11,616] INFO Requested thread factory for component PostgresConnector, id = customers named = blocking-snapshot (io.debezium.util.Threads)
2025-03-20 21:11:11 [2025-03-20 16:11:11,626] INFO Creating thread debezium-postgresconnector-customers-change-event-source-coordinator (io.debezium.util.Threads)
2025-03-20 21:11:11 [2025-03-20 16:11:11,627] INFO WorkerSourceTask{id=pg-connector-0} Source task finished initialization and start (org.apache.kafka.connect.runtime.AbstractWorkerSourceTask)
2025-03-20 21:11:11 [2025-03-20 16:11:11,631] INFO Metrics registered (io.debezium.pipeline.ChangeEventSourceCoordinator)
2025-03-20 21:11:11 [2025-03-20 16:11:11,632] INFO Context created (io.debezium.pipeline.ChangeEventSourceCoordinator)
2025-03-20 21:11:11 [2025-03-20 16:11:11,641] INFO According to the connector configuration data will be snapshotted (io.debezium.connector.postgresql.PostgresSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,646] INFO Snapshot step 1 - Preparing (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,647] INFO Setting isolation level (io.debezium.connector.postgresql.PostgresSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,647] INFO Opening transaction with statement SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
2025-03-20 21:11:11 SET TRANSACTION SNAPSHOT '00000005-00000002-1'; (io.debezium.connector.postgresql.PostgresSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,708] INFO Snapshot step 2 - Determining captured tables (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,711] INFO Created connection pool with 1 threads (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,712] INFO Snapshot step 3 - Locking captured tables [] (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,712] INFO Snapshot step 4 - Determining snapshot offset (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,715] INFO Creating initial offset context (io.debezium.connector.postgresql.PostgresOffsetContext)
2025-03-20 21:11:11 [2025-03-20 16:11:11,718] INFO Read xlogStart at 'LSN{0/1A1C1A8}' from transaction '743' (io.debezium.connector.postgresql.PostgresOffsetContext)
2025-03-20 21:11:11 [2025-03-20 16:11:11,724] INFO Read xlogStart at 'LSN{0/1A1C1A8}' from transaction '743' (io.debezium.connector.postgresql.PostgresSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,724] INFO Snapshot step 5 - Reading structure of captured tables (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,759] INFO Snapshot step 6 - Persisting schema history (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,760] INFO Snapshot step 7 - Snapshotting data (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,762] INFO Creating snapshot worker pool with 1 worker thread(s) (io.debezium.relational.RelationalSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,765] INFO Snapshot - Final stage (io.debezium.pipeline.source.AbstractSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,765] INFO Snapshot completed (io.debezium.pipeline.source.AbstractSnapshotChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,775] INFO Snapshot ended with SnapshotResult [status=COMPLETED, offset=PostgresOffsetContext [sourceInfoSchema=Schema{io.debezium.connector.postgresql.Source:STRUCT}, sourceInfo=source_info[server='customers'db='customers', lsn=LSN{0/1A1C1A8}, txId=743, timestamp=2025-03-20T16:11:11.724Z, snapshot=FALSE, schema=, table=], lastSnapshotRecord=true, lastCompletelyProcessedLsn=null, lastCommitLsn=null, streamingStoppingLsn=null, transactionContext=TransactionContext [currentTransactionId=null, perTableEventCount={}, totalEventCount=0], incrementalSnapshotContext=IncrementalSnapshotContext [windowOpened=false, chunkEndPosition=null, dataCollectionsToSnapshot=[], lastEventKeySent=null, maximumKey=null]]] (io.debezium.pipeline.ChangeEventSourceCoordinator)
2025-03-20 21:11:11 [2025-03-20 16:11:11,775] WARN After applying the include/exclude list filters, no changes will be captured. Please check your configuration! (io.debezium.relational.RelationalDatabaseSchema)
2025-03-20 21:11:11 [2025-03-20 16:11:11,779] INFO Connected metrics set to 'true' (io.debezium.pipeline.ChangeEventSourceCoordinator)
2025-03-20 21:11:11 [2025-03-20 16:11:11,822] INFO SignalProcessor started. Scheduling it every 5000ms (io.debezium.pipeline.signal.SignalProcessor)
2025-03-20 21:11:11 [2025-03-20 16:11:11,822] INFO Creating thread debezium-postgresconnector-customers-SignalProcessor (io.debezium.util.Threads)
2025-03-20 21:11:11 [2025-03-20 16:11:11,823] INFO Starting streaming (io.debezium.pipeline.ChangeEventSourceCoordinator)
2025-03-20 21:11:11 [2025-03-20 16:11:11,823] INFO Retrieved latest position from stored offset 'LSN{0/1A1C1A8}' (io.debezium.connector.postgresql.PostgresStreamingChangeEventSource)
2025-03-20 21:11:11 [2025-03-20 16:11:11,824] INFO Looking for WAL restart position for last commit LSN 'null' and last change LSN 'LSN{0/1A1C1A8}' (io.debezium.connector.postgresql.connection.WalPositionLocator)
2025-03-20 21:11:11 [2025-03-20 16:11:11,835] INFO Obtained valid replication slot ReplicationSlot [active=false, latestFlushedLsn=LSN{0/1A1C1A8}, catalogXmin=743] (io.debezium.connector.postgresql.connection.PostgresConnection)
2025-03-20 21:11:11 [2025-03-20 16:11:11,837] INFO Connection gracefully closed (io.debezium.jdbc.JdbcConnection)
2025-03-20 21:11:11 [2025-03-20 16:11:11,859] INFO Requested thread factory for component PostgresConnector, id = customers named = keep-alive (io.debezium.util.Threads)
2025-03-20 21:11:11 [2025-03-20 16:11:11,860] INFO Creating thread debezium-postgresconnector-customers-keep-alive (io.debezium.util.Threads)
2025-03-20 21:11:11 [2025-03-20 16:11:11,892] INFO Processing messages (io.debezium.connector.postgresql.PostgresStreamingChangeEventSource)

