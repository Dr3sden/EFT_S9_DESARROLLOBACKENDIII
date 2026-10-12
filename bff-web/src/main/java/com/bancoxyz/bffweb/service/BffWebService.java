package com.bancoxyz.bffweb.service;

import com.bancoxyz.bffweb.dto.TransaccionWebDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BffWebService {

    private final RestClient restClient;

    @CircuitBreaker(name = "webBffCB", fallbackMethod = "fallbackTransacciones")
    public List<TransaccionWebDTO> obtenerTransacciones() {
        log.info("[BFF-WEB] Llamando a microservicio-transacciones...");
        TransaccionWebDTO[] response = restClient.get()
                .uri("http://microservicio-transacciones:8081/api/transacciones")
                .retrieve()
                .body(TransaccionWebDTO[].class);
        log.info("[BFF-WEB] Respuesta recibida: {} transacciones",
                response != null ? response.length : 0);
        return response != null ? Arrays.asList(response) : List.of();
    }

    public List<TransaccionWebDTO> fallbackTransacciones(Throwable t) {
        log.warn("[BFF-WEB CB] *** FALLBACK ACTIVADO ***");
        log.warn("[BFF-WEB CB] Excepción: {} - Mensaje: {}",
                t.getClass().getSimpleName(), t.getMessage());
        return List.of();
    }
}
