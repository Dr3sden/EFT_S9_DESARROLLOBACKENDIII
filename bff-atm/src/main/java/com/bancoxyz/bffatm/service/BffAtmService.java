package com.bancoxyz.bffatm.service;

import com.bancoxyz.bffatm.dto.RetiroRequest;
import com.bancoxyz.bffatm.dto.RetiroResponse;
import com.bancoxyz.bffatm.dto.SaldoAtmDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BffAtmService {

    private final RestClient restClient;

    @CircuitBreaker(name = "atmBffCB", fallbackMethod = "fallbackSaldo")
    public SaldoAtmDTO obtenerSaldo(String cuentaId) {
        log.info("[BFF-ATM] Consultando saldo de cuenta: {}", cuentaId);
        Map<?, ?> response = restClient.get()
                .uri("http://microservicio-cuentas:8082/api/cuentas/{numero}", cuentaId)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Cuenta no encontrada: " + cuentaId);
        }

        SaldoAtmDTO dto = new SaldoAtmDTO();
        dto.setCuentaId(cuentaId);
        Object saldo = response.get("saldo");
        dto.setSaldo(saldo != null ? ((Number) saldo).doubleValue() : 0.0);
        return dto;
    }

    public SaldoAtmDTO fallbackSaldo(String cuentaId, Throwable t) {
        log.warn("[BFF-ATM CB] *** FALLBACK SALDO ACTIVADO ***");
        log.warn("[BFF-ATM CB] Cuenta: {} | Excepcion: {} - {}",
                cuentaId, t.getClass().getSimpleName(), t.getMessage());
        return new SaldoAtmDTO(cuentaId, null);
    }

    @CircuitBreaker(name = "atmBffCB", fallbackMethod = "fallbackRetiro")
    public RetiroResponse procesarRetiro(RetiroRequest request) {
        log.info("[BFF-ATM] Procesando retiro: cuenta={} monto={}",
                request.getCuentaId(), request.getMonto());

        Map<?, ?> cuentaActual = restClient.get()
                .uri("http://microservicio-cuentas:8082/api/cuentas/{numero}", request.getCuentaId())
                .retrieve()
                .body(Map.class);

        if (cuentaActual == null) {
            return new RetiroResponse(request.getCuentaId(), 0.0, 0.0, false, "Cuenta no encontrada");
        }

        Object saldoObj = cuentaActual.get("saldo");
        double saldoActual = saldoObj != null ? ((Number) saldoObj).doubleValue() : 0.0;

        if (saldoActual < request.getMonto()) {
            return new RetiroResponse(request.getCuentaId(), 0.0, saldoActual, false,
                    "Saldo insuficiente. Disponible: " + saldoActual);
        }

        Map<?, ?> retiroResult = restClient.post()
                .uri("http://microservicio-cuentas:8082/api/cuentas/{numero}/retiro", request.getCuentaId())
                .body(Map.of("monto", request.getMonto()))
                .retrieve()
                .body(Map.class);

        double nuevoSaldo = retiroResult != null && retiroResult.get("saldo") != null
                ? ((Number) retiroResult.get("saldo")).doubleValue()
                : saldoActual - request.getMonto();

        log.info("[BFF-ATM] Retiro exitoso: cuenta={} saldoFinal={}", request.getCuentaId(), nuevoSaldo);
        return new RetiroResponse(request.getCuentaId(), request.getMonto(), nuevoSaldo, true,
                "Retiro exitoso");
    }

    public RetiroResponse fallbackRetiro(RetiroRequest request, Throwable t) {
        log.warn("[BFF-ATM CB] *** FALLBACK RETIRO ACTIVADO ***");
        log.warn("[BFF-ATM CB] Cuenta: {} | Excepcion: {} - {}",
                request.getCuentaId(), t.getClass().getSimpleName(), t.getMessage());
        return new RetiroResponse(request.getCuentaId(), 0.0, 0.0, false,
                "Servicio temporalmente no disponible. Intente mas tarde.");
    }
}
