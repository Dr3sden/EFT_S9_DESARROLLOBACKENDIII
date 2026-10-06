package com.bancoxyz.cuentas.config;

import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsvDataLoader implements CommandLineRunner {

    private final CuentaRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;

        List<Cuenta> cuentas = new ArrayList<>();
        for (long id = 101; id <= 150; id++) {
            String tipo = (id % 2 == 0) ? "AHORRO" : "PRESTAMO";
            Double tasa = tipo.equals("AHORRO") ? 4.5 : 12.0;
            cuentas.add(new Cuenta(id, "1000" + id, tipo, 1000000.0, tasa, "ACTIVA"));
        }
        repository.saveAll(cuentas);
        log.info("Creadas {} cuentas base", cuentas.size());
    }
}