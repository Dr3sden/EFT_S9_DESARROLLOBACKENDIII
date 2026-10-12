package com.bancoxyz.cuentas.controller;

import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService service;

    @GetMapping
    public List<Cuenta> listar() {
        return service.findAll();
    }

    @GetMapping("/{numeroCuenta}")
    public Cuenta buscar(@PathVariable String numeroCuenta) {
        return service.findByNumero(numeroCuenta);
    }

    /**
     * Endpoint usado por el BFF ATM para procesar retiros.
     * Body: { "monto": 500.0 }
     */
    @PostMapping("/{numeroCuenta}/retiro")
    public ResponseEntity<?> retirar(@PathVariable String numeroCuenta,
                                     @RequestBody Map<String, Object> body) {
        Double monto = body.get("monto") != null
                ? ((Number) body.get("monto")).doubleValue()
                : null;

        try {
            Cuenta cuentaActualizada = service.retirar(numeroCuenta, monto);
            return ResponseEntity.ok(cuentaActualizada);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "numeroCuenta", numeroCuenta
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", e.getMessage(),
                    "numeroCuenta", numeroCuenta
            ));
        }
    }
}
