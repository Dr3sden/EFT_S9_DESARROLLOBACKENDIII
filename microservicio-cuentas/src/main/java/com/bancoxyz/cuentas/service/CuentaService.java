package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository repository;

    @CircuitBreaker(name = "cuentasCB", fallbackMethod = "fallbackFindAll")
    public List<Cuenta> findAll() {
        return repository.findAll();
    }

    @CircuitBreaker(name = "cuentasCB", fallbackMethod = "fallbackFindByNumero")
    public Cuenta findByNumero(String numero) {
        return repository.findByNumeroCuenta(numero)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numero));
    }

    @Transactional
    @CircuitBreaker(name = "cuentasCB", fallbackMethod = "fallbackRetirar")
    public Cuenta retirar(String numero, Double monto) {
        log.info("[MICRO-CUENTAS] Procesando retiro: cuenta={} monto={}", numero, monto);

        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("Monto inválido: " + monto);
        }

        Cuenta cuenta = repository.findByNumeroCuenta(numero)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numero));

        if (cuenta.getSaldo() == null || cuenta.getSaldo() < monto) {
            throw new IllegalStateException("Saldo insuficiente. Disponible: " + cuenta.getSaldo());
        }

        cuenta.setSaldo(cuenta.getSaldo() - monto);
        Cuenta actualizada = repository.save(cuenta);
        log.info("[MICRO-CUENTAS] Retiro OK: cuenta={} saldoNuevo={}", numero, actualizada.getSaldo());
        return actualizada;
    }

    public List<Cuenta> fallbackFindAll(Throwable t) {
        log.warn("[MICRO-CUENTAS CB] Fallback findAll: {}", t.getMessage());
        return List.of();
    }

    public Cuenta fallbackFindByNumero(String numero, Throwable t) {
        log.warn("[MICRO-CUENTAS CB] Fallback findByNumero {}: {}", numero, t.getMessage());
        Cuenta c = new Cuenta();
        c.setNumeroCuenta(numero);
        c.setSaldo(0.0);
        return c;
    }

    public Cuenta fallbackRetirar(String numero, Double monto, Throwable t) {
        log.warn("[MICRO-CUENTAS CB] Fallback retirar {} monto {}: {}", numero, monto, t.getMessage());
        throw new RuntimeException("Servicio de retiro no disponible temporalmente");
    }
}
