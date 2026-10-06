package com.bancoxyz.cuentas.controller;

import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}