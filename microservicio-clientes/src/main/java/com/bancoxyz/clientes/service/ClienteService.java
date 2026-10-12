package com.bancoxyz.clientes.service;

import com.bancoxyz.clientes.dto.ClienteRequest;
import com.bancoxyz.clientes.model.Cliente;
import com.bancoxyz.clientes.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;

    public List<Cliente> findAll() {
        return repository.findAll();
    }

    public Cliente findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + id));
    }

    public Cliente findByRut(String rut) {
        return repository.findByRut(rut)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + rut));
    }

    public Cliente crear(ClienteRequest req) {
        if (repository.findByRut(req.getRut()).isPresent()) {
            throw new IllegalArgumentException("RUT ya registrado: " + req.getRut());
        }

        Cliente c = new Cliente();
        c.setRut(req.getRut());
        c.setNombre(req.getNombre());
        c.setApellido(req.getApellido());
        c.setEmail(req.getEmail());
        c.setTelefono(req.getTelefono());
        c.setDireccion(req.getDireccion());
        c.setEstado("ACTIVO");

        Cliente guardado = repository.save(c);
        log.info("[CLIENTES] Cliente creado: rut={} id={}", guardado.getRut(), guardado.getId());
        return guardado;
    }

    public Cliente actualizar(Long id, ClienteRequest req) {
        Cliente c = findById(id);
        c.setNombre(req.getNombre());
        c.setApellido(req.getApellido());
        c.setEmail(req.getEmail());
        c.setTelefono(req.getTelefono());
        c.setDireccion(req.getDireccion());
        return repository.save(c);
    }

    public void eliminar(Long id) {
        Cliente c = findById(id);
        c.setEstado("INACTIVO");
        repository.save(c);
        log.info("[CLIENTES] Cliente marcado INACTIVO: id={}", id);
    }
}
