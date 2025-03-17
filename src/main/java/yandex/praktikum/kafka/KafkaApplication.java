package yandex.praktikum.kafka;


import yandex.praktikum.kafka.connector.Producer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaApplication {

    public static void main(String[] args) {
        // Запуск продьюсера
        ExecutorService producerExecutorService = Executors.newSingleThreadExecutor();
        producerExecutorService.submit(() -> new Producer().send());
    }
}
