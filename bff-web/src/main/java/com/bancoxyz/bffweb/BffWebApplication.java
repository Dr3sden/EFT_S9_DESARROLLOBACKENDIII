package com.bancoxyz.bffweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BffWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffWebApplication.class, args);
    }
}