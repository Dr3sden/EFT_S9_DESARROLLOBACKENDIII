package com.bancoxyz.pagos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PagoRequest {

    @NotBlank(message = "Cuenta origen obligatoria")
    private String cuentaOrigen;

    private String cuentaDestino;   // null si es DEPOSITO

    @NotNull(message = "Monto obligatorio")
    @Min(value = 1, message = "Monto minimo 1")
    private Double monto;

    private String descripcion;
}
