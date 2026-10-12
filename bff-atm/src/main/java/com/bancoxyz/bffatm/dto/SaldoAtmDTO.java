package com.bancoxyz.bffatm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaldoAtmDTO {
    private String cuentaId;
    private Double saldo;
}
