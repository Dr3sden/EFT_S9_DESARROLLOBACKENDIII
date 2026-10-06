package com.bancoxyz.transacciones.repository;

import com.bancoxyz.transacciones.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    List<Transaccion> findTop10ByOrderByFechaDesc();
}