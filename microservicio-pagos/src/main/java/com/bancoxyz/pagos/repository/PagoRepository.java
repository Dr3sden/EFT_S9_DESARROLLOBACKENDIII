package com.bancoxyz.pagos.repository;

import com.bancoxyz.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByCuentaOrigen(String cuentaOrigen);
    List<Pago> findByEstado(String estado);
}
