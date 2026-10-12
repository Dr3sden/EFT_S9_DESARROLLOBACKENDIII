package com.bancoxyz.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank(message = "RUT obligatorio")
    private String rut;

    @NotBlank(message = "Nombre obligatorio")
    private String nombre;

    @NotBlank(message = "Apellido obligatorio")
    private String apellido;

    @Email(message = "Email invalido")
    private String email;

    private String telefono;
    private String direccion;
}
