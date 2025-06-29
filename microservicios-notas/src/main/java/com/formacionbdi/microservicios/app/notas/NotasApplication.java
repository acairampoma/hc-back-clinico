package com.formacionbdi.microservicios.app.notas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NotasApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotasApplication.class, args);
    }

}
