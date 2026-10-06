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

    @CircuitBreaker(name = "webBffCB", fallbackMethod = "fallback")
    public List<TransaccionWebDTO> obtenerTransacciones() {
        TransaccionWebDTO[] response = restClient.get()
                .uri("http://microservicio-transacciones:8081/api/transacciones")
                .retrieve()
                .body(TransaccionWebDTO[].class);
        return response != null ? Arrays.asList(response) : List.of();
    }

    public List<TransaccionWebDTO> fallback(Throwable t) {
        log.warn("[BFF-WEB CB] Fallback activado: {}", t.getMessage());
        return List.of();
    }
}