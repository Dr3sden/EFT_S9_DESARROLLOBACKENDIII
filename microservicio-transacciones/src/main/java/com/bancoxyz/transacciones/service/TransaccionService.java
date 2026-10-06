package com.bancoxyz.transacciones.service;

import com.bancoxyz.transacciones.model.Transaccion;
import com.bancoxyz.transacciones.repository.TransaccionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransaccionService {

    private final TransaccionRepository repository;
    private final KafkaProducerService kafkaProducer;

    @CircuitBreaker(name = "transaccionesCB", fallbackMethod = "fallbackFindAll")
    public List<Transaccion> findAll() {
        return repository.findAll();
    }

    @CircuitBreaker(name = "transaccionesCB", fallbackMethod = "fallbackFindById")
    public Transaccion findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));
    }

    public Transaccion save(Transaccion t) {
        Transaccion saved = repository.save(t);
        kafkaProducer.enviarEvento(saved);
        return saved;
    }

    public List<Transaccion> fallbackFindAll(Throwable t) {
        log.warn("[CB-FALLBACK] findAll falló: {}", t.getMessage());
        return List.of();
    }

    public Transaccion fallbackFindById(Long id, Throwable t) {
        log.warn("[CB-FALLBACK] findById({}) falló: {}", id, t.getMessage());
        Transaccion t2 = new Transaccion();
        t2.setId(id);
        t2.setTipo("FALLBACK");
        t2.setMonto(0.0);
        return t2;
    }
}