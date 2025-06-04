package com.formacionbdi.microservicios.app.listas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableEurekaClient
public class ListasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListasApplication.class, args);
    }

}
