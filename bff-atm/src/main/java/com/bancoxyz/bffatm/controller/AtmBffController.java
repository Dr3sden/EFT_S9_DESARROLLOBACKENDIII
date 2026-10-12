package com.bancoxyz.bffatm.controller;

import com.bancoxyz.bffatm.dto.RetiroRequest;
import com.bancoxyz.bffatm.dto.RetiroResponse;
import com.bancoxyz.bffatm.dto.SaldoAtmDTO;
import com.bancoxyz.bffatm.service.BffAtmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm")
@RequiredArgsConstructor
public class AtmBffController {

    private final BffAtmService service;

    @GetMapping("/saldo/{cuentaId}")
    public ResponseEntity<SaldoAtmDTO> obtenerSaldo(@PathVariable String cuentaId) {
        return ResponseEntity.ok(service.obtenerSaldo(cuentaId));
    }

    @PostMapping("/retiro")
    public ResponseEntity<RetiroResponse> retirar(@Valid @RequestBody RetiroRequest request) {
        RetiroResponse response = service.procesarRetiro(request);
        if (!response.isExitoso()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
