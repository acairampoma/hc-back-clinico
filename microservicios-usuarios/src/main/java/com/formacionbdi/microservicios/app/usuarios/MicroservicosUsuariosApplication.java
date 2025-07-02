package com.formacionbdi.microservicios.app.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients
@SpringBootApplication
@ComponentScan({"com.formacionbdi.microservicios.app.usuarios"})
public class MicroservicosUsuariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicosUsuariosApplication.class, args);
	}

}
