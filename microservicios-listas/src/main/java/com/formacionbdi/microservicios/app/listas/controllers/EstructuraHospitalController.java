package com.formacionbdi.microservicios.app.listas.controllers;

import com.formacionbdi.microservicios.app.listas.models.dto.EstructuraHospitalDTO;
import com.formacionbdi.microservicios.app.listas.models.response.ApiResponse;
import com.formacionbdi.microservicios.app.listas.services.EstructuraHospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.List;

/**
 * Controlador REST para gestión de estructura hospitalaria
 */
@RestController
@RequestMapping("/listas")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EstructuraHospitalController {

    private final EstructuraHospitalService estructuraService;

    /**
     * Obtiene todas las estructuras hospitalarias
     * GET /estructuras
     */
    @GetMapping("/estructuras")
    public ResponseEntity<ApiResponse<List<EstructuraHospitalDTO>>> obtenerTodasLasEstructuras() {
        log.info("REST request para obtener todas las estructuras hospitalarias");

        List<EstructuraHospitalDTO> estructuras = estructuraService.obtenerTodasLasEstructuras();

        ApiResponse<List<EstructuraHospitalDTO>> response = ApiResponse.success(
                estructuras,
                "Estructuras hospitalarias obtenidas exitosamente"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Obtiene la primera estructura hospitalaria disponible
     * GET /estructura
     */
    @GetMapping("/estructura")
    public ResponseEntity<ApiResponse<EstructuraHospitalDTO>> obtenerPrimeraEstructura() {
        log.info("REST request para obtener primera estructura hospitalaria");

        EstructuraHospitalDTO estructura = estructuraService.obtenerPrimeraEstructura();

        ApiResponse<EstructuraHospitalDTO> response = ApiResponse.success(
                estructura,
                "Estructura hospitalaria obtenida exitosamente"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Obtiene estructura hospitalaria por ID específico
     * GET /estructura/{hospitalId}
     */
    @GetMapping("/estructura/{hospitalId}")
    public ResponseEntity<ApiResponse<EstructuraHospitalDTO>> obtenerEstructuraPorHospitalId(
            @PathVariable @Min(value = 1, message = "El ID del hospital debe ser mayor a 0") Long hospitalId) {

        log.info("REST request para obtener estructura del hospital ID: {}", hospitalId);

        EstructuraHospitalDTO estructura = estructuraService.obtenerEstructuraPorHospitalId(hospitalId);

        ApiResponse<EstructuraHospitalDTO> response = ApiResponse.success(
                estructura,
                "Estructura del hospital obtenida exitosamente"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Obtiene estructura básica sin detalle de camas
     * GET /estructura/basica
     */
    @GetMapping("/estructura/basica")
    public ResponseEntity<ApiResponse<EstructuraHospitalDTO>> obtenerEstructuraBasica() {
        log.info("REST request para obtener estructura básica");

        EstructuraHospitalDTO estructura = estructuraService.obtenerEstructuraBasica();

        ApiResponse<EstructuraHospitalDTO> response = ApiResponse.success(
                estructura,
                "Estructura básica obtenida exitosamente"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Verifica si existe estructura para un hospital específico
     * GET /estructura/{hospitalId}/existe
     */
    @GetMapping("/estructura/{hospitalId}/existe")
    public ResponseEntity<ApiResponse<Boolean>> verificarExistenciaEstructura(
            @PathVariable @Min(value = 1, message = "El ID del hospital debe ser mayor a 0") Long hospitalId) {

        log.info("REST request para verificar existencia de estructura del hospital ID: {}", hospitalId);

        boolean existe = estructuraService.existeEstructuraPorHospitalId(hospitalId);

        ApiResponse<Boolean> response = ApiResponse.success(
                existe,
                existe ? "El hospital tiene estructura configurada" : "El hospital no tiene estructura configurada"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Obtiene estadísticas de disponibilidad de camas
     * GET /estadisticas/disponibilidad
     */
    @GetMapping("/estadisticas/disponibilidad")
    public ResponseEntity<ApiResponse<Object>> obtenerEstadisticasDisponibilidad() {
        log.info("REST request para obtener estadísticas de disponibilidad");

        Object estadisticas = estructuraService.obtenerEstadisticasDisponibilidad();

        ApiResponse<Object> response = ApiResponse.success(
                estadisticas,
                "Estadísticas de disponibilidad obtenidas exitosamente"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Health Check del microservicio
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        log.info("Health check del microservicio de listas");

        ApiResponse<String> response = ApiResponse.success(
                "Microservicio de listas funcionando correctamente",
                "Health check exitoso"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Información del microservicio
     * GET /info
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Object>> obtenerInformacion() {
        log.info("REST request para obtener información del microservicio");

        var info = new Object() {
            public final String nombre = "Microservicio de Listas Hospitalarias";
            public final String version = "1.0.0";
            public final String descripcion = "Microservicio para consultar estructuras hospitalarias";
            public final String[] endpoints = {
                    "GET /estructuras - Todas las estructuras",
                    "GET /estructura - Primera estructura",
                    "GET /estructura/{id} - Estructura por hospital ID",
                    "GET /estructura/basica - Estructura básica",
                    "GET /estructura/{id}/existe - Verificar existencia",
                    "GET /estadisticas/disponibilidad - Estadísticas",
                    "GET /health - Health check",
                    "GET /info - Información del servicio"
            };
        };

        ApiResponse<Object> response = ApiResponse.success(
                info,
                "Información del microservicio obtenida exitosamente"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}