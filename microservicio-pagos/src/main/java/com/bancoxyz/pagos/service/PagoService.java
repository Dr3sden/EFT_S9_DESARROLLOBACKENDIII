package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.dto.PagoRequest;
import com.bancoxyz.pagos.model.Pago;
import com.bancoxyz.pagos.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository repository;
    private final KafkaProducerService kafkaProducer;

    public List<Pago> findAll() {
        return repository.findAll();
    }

    public Pago findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + id));
    }

    public List<Pago> findByCuenta(String cuenta) {
        return repository.findByCuentaOrigen(cuenta);
    }

    /**
     * Procesa una transferencia entre cuentas.
     */
    public Pago procesarTransferencia(PagoRequest req) {
        if (req.getCuentaDestino() == null || req.getCuentaDestino().isBlank()) {
            throw new IllegalArgumentException("Cuenta destino obligatoria para transferencia");
        }

        Pago pago = new Pago();
        pago.setCuentaOrigen(req.getCuentaOrigen());
        pago.setCuentaDestino(req.getCuentaDestino());
        pago.setMonto(req.getMonto());
        pago.setTipo("TRANSFERENCIA");
        pago.setEstado("COMPLETADO");
        pago.setFecha(LocalDateTime.now());
        pago.setDescripcion(req.getDescripcion());

        Pago guardado = repository.save(pago);
        kafkaProducer.publicarPago(guardado);
        log.info("[PAGOS] Transferencia OK: {} -> {} monto={}",
                req.getCuentaOrigen(), req.getCuentaDestino(), req.getMonto());
        return guardado;
    }

    /**
     * Procesa un depósito en una cuenta.
     */
    public Pago procesarDeposito(PagoRequest req) {
        Pago pago = new Pago();
        pago.setCuentaOrigen("N/A");  // depósito no tiene origen
        pago.setCuentaDestino(req.getCuentaOrigen());  // en deposito, cuentaOrigen del request es el destino
        pago.setMonto(req.getMonto());
        pago.setTipo("DEPOSITO");
        pago.setEstado("COMPLETADO");
        pago.setFecha(LocalDateTime.now());
        pago.setDescripcion(req.getDescripcion());

        Pago guardado = repository.save(pago);
        kafkaProducer.publicarPago(guardado);
        log.info("[PAGOS] Deposito OK: cuenta={} monto={}", req.getCuentaOrigen(), req.getMonto());
        return guardado;
    }
}
