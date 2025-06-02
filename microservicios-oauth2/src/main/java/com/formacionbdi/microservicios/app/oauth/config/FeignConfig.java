package com.formacionbdi.microservicios.app.oauth.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // Para debugging, cambiar a BASIC en producción
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5000, TimeUnit.MILLISECONDS, // connection timeout
                10000, TimeUnit.MILLISECONDS, // read timeout
                true // follow redirects
        );
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                100, // period inicial
                1000, // max period
                3 // max attempts
        );
    }
}