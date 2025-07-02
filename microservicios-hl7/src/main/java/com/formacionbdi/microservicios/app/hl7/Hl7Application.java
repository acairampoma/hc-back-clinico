package com.formacionbdi.microservicios.app.hl7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;

/**
 * Microservicio HL7 - Sistema Hospitalario
 * Procesamiento de mensajes HL7 v2.5 con protocolo MLLP
 * 
 * Funcionalidades:
 * - Servidor HL7 en puerto 2575 (INBOUND)
 * - Cliente HL7 para envos (OUTBOUND)
 * - Procesamiento de mensajes ADT, ORU, ORM, SIU, RDE, MDM
 * - Integracin con base de datos PostgreSQL
 * - API REST para consultas en puerto 8004
 * - Monitoreo y mtricas con Actuator
 * 
 * @author Alan Cairampoma
 * @version 1.0
 * @since 2025-06-19
 */
@SpringBootApplication
@EnableFeignClients
@EnableEurekaClient
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = "com.formacionbdi.microservicios.app.hl7")
public class Hl7Application {

    public static void main(String[] args) {
        System.out.println("?? Iniciando Microservicio HL7...");
        System.out.println("?? Puerto HL7 MLLP: 2575");
        System.out.println("?? Puerto REST API: 8004");
        System.out.println("?? Base de datos: bd_hdigital");
        System.out.println("?? Eureka Discovery: Activo");
        
        SpringApplication.run(Hl7Application.class, args);
        
        System.out.println("? Microservicio HL7 iniciado exitosamente!");
        System.out.println("?? Health Check: http://localhost:8004/actuator/health");
        System.out.println("?? M��tricas: http://localhost:8004/actuator/metrics");
    }
}
