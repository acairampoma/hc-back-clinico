package com.formacionbdi.microservicios.app.receta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class RecetaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecetaApplication.class, args);
    }

}
