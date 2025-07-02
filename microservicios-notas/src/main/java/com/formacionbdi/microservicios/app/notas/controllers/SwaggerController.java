package com.formacionbdi.microservicios.app.notas.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class SwaggerController {

    @Value("${server.port}")
    private String serverPort;

    /**
     * Endpoint especial para permitir acceso a la documentación OpenAPI desde el API Gateway
     * Este método actúa como un proxy para evitar problemas de CORS
     */
    @GetMapping("/api-docs")
    public ResponseEntity<Object> getApiDocs() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:" + serverPort + "/v3/api-docs";
            Object apiDocs = restTemplate.getForObject(url, Object.class);
            return new ResponseEntity<>(apiDocs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al obtener documentación OpenAPI: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
