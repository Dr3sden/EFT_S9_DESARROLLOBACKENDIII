package com.bancoxyz.bffweb.controller;

import com.bancoxyz.bffweb.dto.TransaccionWebDTO;
import com.bancoxyz.bffweb.service.BffWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class WebBffController {

    private final BffWebService service;

    @GetMapping("/transacciones")
    public List<TransaccionWebDTO> transacciones() {
        return service.obtenerTransacciones();
    }
}