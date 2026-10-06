package com.bancoxyz.transacciones.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "transacciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaccion {
    @Id
    @Column(name = "id_transaccion")
    private Long id;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "monto")
    private Double monto;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "anomalia")
    private Boolean anomalia;

    @Column(name = "fecha_procesamiento")
    private LocalDate fechaProcesamiento;
}