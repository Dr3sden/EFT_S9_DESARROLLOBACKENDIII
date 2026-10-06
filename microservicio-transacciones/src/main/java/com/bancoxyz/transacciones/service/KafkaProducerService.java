package com.bancoxyz.transacciones.service;

import com.bancoxyz.transacciones.model.Transaccion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void enviarEvento(Transaccion transaccion) {
        log.info("[KAFKA-PRODUCER] Enviando transacción ID: {}", transaccion.getId());
        kafkaTemplate.send("transacciones-topic", String.valueOf(transaccion.getId()), transaccion);
    }
}