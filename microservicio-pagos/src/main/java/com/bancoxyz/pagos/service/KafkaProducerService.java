package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.model.Pago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private static final String TOPIC = "pagos-topic";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publicarPago(Pago pago) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(pago.getId()), pago);
            log.info("[PAGOS-KAFKA] Evento publicado en {}: id={} monto={}",
                    TOPIC, pago.getId(), pago.getMonto());
        } catch (Exception e) {
            log.error("[PAGOS-KAFKA] Error publicando evento: {}", e.getMessage());
        }
    }
}
