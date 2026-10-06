package com.bancoxyz.cuentas.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransaccionConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "transacciones-topic", groupId = "banco-xyz-group-cuentas-v2")
    public void consumir(String mensajeJson) {
        try {
            JsonNode transaccion = objectMapper.readTree(mensajeJson);
            log.info("✅ [KAFKA-CONSUMER] Evento recibido: {}", transaccion.toPrettyString());
        } catch (Exception e) {
            log.error("❌ [KAFKA-CONSUMER] Error parseando JSON: {}", mensajeJson, e);
        }
    }
}