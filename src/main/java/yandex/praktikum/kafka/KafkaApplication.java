package yandex.praktikum.kafka;


import lombok.extern.slf4j.Slf4j;
import yandex.praktikum.kafka.connector.Producer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class KafkaApplication {

    public static void main(String[] args) {
        log.info("Application started");
         //Запуск продьюсера
        ExecutorService producerExecutorService = Executors.newSingleThreadExecutor();
        producerExecutorService.submit(() -> new Producer().send());
    }
}
