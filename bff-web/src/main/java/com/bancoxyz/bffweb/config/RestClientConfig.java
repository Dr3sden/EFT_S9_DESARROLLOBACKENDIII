package com.bancoxyz.bffweb.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);   // 3 segundos para conectar
        factory.setReadTimeout(5000);      // 5 segundos para leer respuesta

        return RestClient.builder()
            .requestFactory(factory)
            .requestInterceptor((request, body, execution) -> {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest servletRequest = attrs.getRequest();
                    String authHeader = servletRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        request.getHeaders().add("Authorization", authHeader);
                        log.debug("[BFF-WEB] Propagando JWT a: {}", request.getURI());
                    }
                }
                return execution.execute(request, body);
            })
            .build();
    }
}
