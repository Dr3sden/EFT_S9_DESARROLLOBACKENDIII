package com.bancoxyz.bffweb.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TransaccionWebDTO {
    private Long id;
    private String tipo;
    private Double monto;
    private LocalDate fecha;
    private Boolean anomalia;
    private LocalDate fechaProcesamiento;
}