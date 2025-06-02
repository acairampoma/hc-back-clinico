package com.medical.clinic.test.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/hola")
    public ResponseEntity<?> holaMundo(@RequestParam(name = "nombre", required = false) String nombre) {
        Map<String, String> respuesta = new HashMap<>();
        String mensaje = (nombre != null && !nombre.isEmpty()) ? 
                        "¡Hola " + nombre + "! Bienvenido al microservicio de prueba." : 
                        "¡Hola Mundo! Este es un microservicio de prueba.";
        
        respuesta.put("mensaje", mensaje);
        respuesta.put("estado", "éxito");
        
        return ResponseEntity.ok(respuesta);
    }
}
