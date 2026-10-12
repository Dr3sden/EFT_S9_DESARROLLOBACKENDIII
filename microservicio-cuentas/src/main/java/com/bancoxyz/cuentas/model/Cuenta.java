package com.bancoxyz.cuentas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuentas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cuenta {

    @Id
    @Column(name = "id_cuenta")
    private Long id;

    @Column(name = "numero_cuenta", unique = true)
    private String numeroCuenta;

    @Column(name = "tipo_cuenta")
    private String tipoCuenta;

    @Column(name = "saldo")
    private Double saldo;

    @Column(name = "tasa_interes")
    private Double tasaInteres;

    @Column(name = "estado")
    private String estado;
}
