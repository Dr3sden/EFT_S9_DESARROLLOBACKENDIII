package com.bancoxyz.transacciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TransaccionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransaccionesApplication.class, args);
    }
}