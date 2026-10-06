package com.bancoxyz.bffmovil.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TransaccionMobileDTO {
    private Long id;
    private String tipo;
    private Double monto;
    private LocalDate fecha;
}