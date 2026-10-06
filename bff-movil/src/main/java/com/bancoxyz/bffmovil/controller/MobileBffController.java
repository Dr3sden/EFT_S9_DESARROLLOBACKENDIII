package com.bancoxyz.bffmovil.controller;

import com.bancoxyz.bffmovil.dto.TransaccionMobileDTO;
import com.bancoxyz.bffmovil.service.BffMovilService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileBffController {

    private final BffMovilService service;

    @GetMapping("/transacciones/recientes")
    public List<TransaccionMobileDTO> recientes(@RequestParam(defaultValue = "5") int limit) {
        return service.obtenerRecientes(limit);
    }
}