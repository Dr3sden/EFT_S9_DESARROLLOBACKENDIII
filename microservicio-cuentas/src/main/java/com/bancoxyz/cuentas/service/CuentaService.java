package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
    }

    public List<Cuenta> fallbackFindAll(Throwable t) {
        return List.of();
    }

    public Cuenta fallbackFindByNumero(String numero, Throwable t) {
        Cuenta c = new Cuenta();
        c.setNumeroCuenta(numero);
        c.setSaldo(0.0);
        return c;
    }
}