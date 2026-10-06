package com.bancoxyz.bffmovil.service;

import com.bancoxyz.bffmovil.dto.TransaccionMobileDTO;
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
public class BffMovilService {

    private final RestClient restClient;

    @CircuitBreaker(name = "mobileBffCB", fallbackMethod = "fallback")
    public List<TransaccionMobileDTO> obtenerRecientes(int limit) {
        TransaccionMobileDTO[] response = restClient.get()
                .uri("http://microservicio-transacciones:8081/api/transacciones")
                .retrieve()
                .body(TransaccionMobileDTO[].class);
        if (response == null) return List.of();
        return Arrays.stream(response).limit(limit).toList();
    }

    public List<TransaccionMobileDTO> fallback(int limit, Throwable t) {
        log.warn("[BFF-MOVIL CB] Fallback activado: {}", t.getMessage());
        return List.of();
    }
}