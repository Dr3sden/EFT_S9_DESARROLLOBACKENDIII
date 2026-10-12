package com.bancoxyz.bffatm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetiroResponse {
    private String cuentaId;
    private Double montoRetirado;
    private Double saldoRestante;
    private boolean exitoso;
    private String mensaje;
}
