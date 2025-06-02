package com.formacionbdi.microservicios.app.oauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@EnableDiscoveryClient
@SpringBootApplication
public class OauthApplication implements CommandLineRunner {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static void main(String[] args) {
        SpringApplication.run(OauthApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String password = "123456";

        System.out.println("\n========================================");
        System.out.println("Generando 5 claves BCrypt para: " + password);
        System.out.println("========================================");

        for (int i = 1; i <= 5; i++) {
            String encodedPassword = passwordEncoder.encode(password);
            System.out.println("Clave " + i + ": " + encodedPassword);
        }

        System.out.println("========================================\n");
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}