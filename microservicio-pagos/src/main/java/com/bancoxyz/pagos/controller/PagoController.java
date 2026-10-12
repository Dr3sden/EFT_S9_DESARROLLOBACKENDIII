package com.bancoxyz.pagos.controller;

import com.bancoxyz.pagos.dto.PagoRequest;
import com.bancoxyz.pagos.model.Pago;
import com.bancoxyz.pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService service;

    @GetMapping
    public List<Pago> listar() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Pago buscar(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/cuenta/{cuentaOrigen}")
    public List<Pago> porCuenta(@PathVariable String cuentaOrigen) {
        return service.findByCuenta(cuentaOrigen);
    }

    @PostMapping("/transferencia")
    public ResponseEntity<Pago> transferencia(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(service.procesarTransferencia(request));
    }

    @PostMapping("/deposito")
    public ResponseEntity<Pago> deposito(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(service.procesarDeposito(request));
    }
}
