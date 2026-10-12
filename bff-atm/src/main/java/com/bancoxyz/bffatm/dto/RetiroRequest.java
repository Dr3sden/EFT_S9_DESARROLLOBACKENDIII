package com.bancoxyz.bffatm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RetiroRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    private String cuentaId;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto minimo es 1")
    private Double monto;
}
