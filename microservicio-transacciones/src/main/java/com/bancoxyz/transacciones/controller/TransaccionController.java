package com.bancoxyz.transacciones.controller;

import com.bancoxyz.transacciones.model.Transaccion;
import com.bancoxyz.transacciones.service.TransaccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService service;

    @GetMapping
    public List<Transaccion> listar() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Transaccion buscar(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public Transaccion crear(@RequestBody Transaccion t) {
        return service.save(t);
    }
}