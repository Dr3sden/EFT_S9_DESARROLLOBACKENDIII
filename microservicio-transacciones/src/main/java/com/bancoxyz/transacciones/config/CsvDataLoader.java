package com.bancoxyz.transacciones.config;

import com.bancoxyz.transacciones.model.Transaccion;
import com.bancoxyz.transacciones.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsvDataLoader implements CommandLineRunner {

    private final TransaccionRepository repository;

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;
        log.info("=== CARGANDO TRANSACCIONES DEL LEGACY ===");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("transacciones.csv").getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            List<Transaccion> lista = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] c = line.split(",", -1);
                if (c.length < 4) continue;

                try {
                    Long id = Long.parseLong(c[0].trim());
                    LocalDate fecha = parseFecha(c[1].trim());
                    if (fecha == null || c[2].trim().isEmpty()) continue;

                    Double monto = Math.abs(Double.parseDouble(c[2].trim()));
                    String tipo = c[3].trim().toLowerCase();
                    if (!tipo.equals("credito") && !tipo.equals("debito")) continue;

                    String tipoFinal = tipo.equals("credito") ? "DEPOSITO" : "RETIRO";
                    boolean anomalia = Double.parseDouble(c[2].trim()) < 0;

                    lista.add(new Transaccion(id, tipoFinal, monto, fecha, anomalia, LocalDate.now()));
                } catch (Exception ignored) {}
            }

            repository.saveAll(lista);
            log.info("Cargadas {} transacciones", lista.size());
        } catch (Exception e) {
            log.error("Error cargando transacciones: {}", e.getMessage());
        }
    }

    private LocalDate parseFecha(String s) {
        for (DateTimeFormatter f : FORMATTERS) {
            try { return LocalDate.parse(s, f); } catch (Exception ignored) {}
        }
        return null;
    }
}