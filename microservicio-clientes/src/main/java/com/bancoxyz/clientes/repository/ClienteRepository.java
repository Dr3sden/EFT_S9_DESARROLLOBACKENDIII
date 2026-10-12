package com.bancoxyz.clientes.repository;

import com.bancoxyz.clientes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByRut(String rut);
    Optional<Cliente> findByEmail(String email);
}
