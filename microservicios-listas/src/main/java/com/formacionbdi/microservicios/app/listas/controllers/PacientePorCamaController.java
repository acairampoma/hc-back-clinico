package com.formacionbdi.microservicios.app.listas.controllers;

import com.formacionbdi.microservicios.app.listas.models.dto.PacientePorCamaDTO;
import com.formacionbdi.microservicios.commons.response.ApiResponse;
import com.formacionbdi.microservicios.app.listas.services.PacientePorCamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de pacientes por cama
 */
@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
@Slf4j
public class PacientePorCamaController {

    private final PacientePorCamaService pacientePorCamaService;

    /**
     * Obtiene todas las camas con información de pacientes
     * GET /pacientes/camas
     */
    @GetMapping("/camas")
    public ResponseEntity<ApiResponse<List<PacientePorCamaDTO>>> obtenerTodasLasCamas() {
        log.info("REST request para obtener todas las camas con información de pacientes");

        List<PacientePorCamaDTO> camas = pacientePorCamaService.obtenerTodasLasCamas();

        return ResponseEntity.ok(
                ApiResponse.success(camas, "Camas obtenidas exitosamente")
        );
    }

    /**
     * Obtiene solo las camas ocupadas (con pacientes)
     * GET /pacientes/camas/ocupadas
     */
    @GetMapping("/camas/ocupadas")
    public ResponseEntity<ApiResponse<List<PacientePorCamaDTO>>> obtenerCamasOcupadas() {
        log.info("REST request para obtener camas ocupadas");

        List<PacientePorCamaDTO> camasOcupadas = pacientePorCamaService.obtenerCamasOcupadas();

        return ResponseEntity.ok(
                ApiResponse.success(camasOcupadas, "Camas ocupadas obtenidas exitosamente")
        );
    }

    /**
     * Obtiene solo las camas disponibles (sin pacientes)
     * GET /pacientes/camas/disponibles
     */
    @GetMapping("/camas/disponibles")
    public ResponseEntity<ApiResponse<List<PacientePorCamaDTO>>> obtenerCamasDisponibles() {
        log.info("REST request para obtener camas disponibles");

        List<PacientePorCamaDTO> camasDisponibles = pacientePorCamaService.obtenerCamasDisponibles();

        return ResponseEntity.ok(
                ApiResponse.success(camasDisponibles, "Camas disponibles obtenidas exitosamente")
        );
    }

    /**
     * Obtiene información de una cama específica por número
     * GET /pacientes/cama/{bedNumber}
     */
    @GetMapping("/cama/{bedNumber}")
    public ResponseEntity<ApiResponse<PacientePorCamaDTO>> obtenerCamaPorNumero(
            @PathVariable String bedNumber) {
        log.info("REST request para obtener cama por número: {}", bedNumber);

        PacientePorCamaDTO cama = pacientePorCamaService.obtenerCamaPorNumero(bedNumber);

        return ResponseEntity.ok(
                ApiResponse.success(cama, "Cama encontrada exitosamente")
        );
    }

    /**
     * Obtiene camas en un rango específico
     * GET /pacientes/camas/rango?start={startBed}&end={endBed}
     */
    @GetMapping("/camas/rango")
    public ResponseEntity<ApiResponse<List<PacientePorCamaDTO>>> obtenerCamasPorRango(
            @RequestParam String start,
            @RequestParam String end) {
        log.info("REST request para obtener camas en rango: {} - {}", start, end);

        List<PacientePorCamaDTO> camas = pacientePorCamaService.obtenerCamasPorRango(start, end);

        return ResponseEntity.ok(
                ApiResponse.success(camas, "Camas en rango obtenidas exitosamente")
        );
    }

    /**
     * Busca un paciente por su DNI
     * GET /pacientes/buscar/dni/{dni}
     */
    @GetMapping("/buscar/dni/{dni}")
    public ResponseEntity<ApiResponse<PacientePorCamaDTO>> buscarPacientePorDni(
            @PathVariable String dni) {
        log.info("REST request para buscar paciente por DNI: {}", dni);

        PacientePorCamaDTO paciente = pacientePorCamaService.buscarPacientePorDni(dni);

        return ResponseEntity.ok(
                ApiResponse.success(paciente, "Paciente encontrado exitosamente")
        );
    }

    /**
     * Busca pacientes por nombre (búsqueda parcial)
     * GET /pacientes/buscar/nombre/{nombre}
     */
    @GetMapping("/buscar/nombre/{nombre}")
    public ResponseEntity<ApiResponse<List<PacientePorCamaDTO>>> buscarPacientesPorNombre(
            @PathVariable String nombre) {
        log.info("REST request para buscar pacientes por nombre: {}", nombre);

        List<PacientePorCamaDTO> pacientes = pacientePorCamaService.buscarPacientesPorNombre(nombre);

        return ResponseEntity.ok(
                ApiResponse.success(pacientes, "Pacientes encontrados exitosamente")
        );
    }

    /**
     * Busca pacientes por médico tratante
     * GET /pacientes/buscar/medico/{medico}
     */
    @GetMapping("/buscar/medico/{medico}")
    public ResponseEntity<ApiResponse<List<PacientePorCamaDTO>>> buscarPacientesPorMedico(
            @PathVariable String medico) {
        log.info("REST request para buscar pacientes por médico: {}", medico);

        List<PacientePorCamaDTO> pacientes = pacientePorCamaService.buscarPacientesPorMedico(medico);

        return ResponseEntity.ok(
                ApiResponse.success(pacientes, "Pacientes encontrados exitosamente")
        );
    }

    /**
     * Obtiene estadísticas completas de ocupación de camas
     * GET /pacientes/estadisticas/ocupacion
     */
    @GetMapping("/estadisticas/ocupacion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadisticasOcupacion() {
        log.info("REST request para obtener estadísticas de ocupación");

        Map<String, Object> estadisticas = pacientePorCamaService.obtenerEstadisticasOcupacion();

        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Estadísticas obtenidas exitosamente")
        );
    }

    /**
     * Verifica si una cama específica está ocupada
     * GET /pacientes/cama/{bedNumber}/ocupada
     */
    @GetMapping("/cama/{bedNumber}/ocupada")
    public ResponseEntity<ApiResponse<Boolean>> isCamaOcupada(@PathVariable String bedNumber) {
        log.info("REST request para verificar si cama {} está ocupada", bedNumber);

        boolean ocupada = pacientePorCamaService.isCamaOcupada(bedNumber);

        return ResponseEntity.ok(
                ApiResponse.success(ocupada, "Estado de cama verificado")
        );
    }

    /**
     * Verifica si existe una cama con el número especificado
     * GET /pacientes/cama/{bedNumber}/existe
     */
    @GetMapping("/cama/{bedNumber}/existe")
    public ResponseEntity<ApiResponse<Boolean>> existeCama(@PathVariable String bedNumber) {
        log.info("REST request para verificar si existe cama: {}", bedNumber);

        boolean existe = pacientePorCamaService.existeCama(bedNumber);

        return ResponseEntity.ok(
                ApiResponse.success(existe, "Existencia de cama verificada")
        );
    }

    /**
     * Obtiene el conteo de camas por estado
     * GET /pacientes/estadisticas/conteos
     */
    @GetMapping("/estadisticas/conteos")
    public ResponseEntity<ApiResponse<Map<String, Long>>> obtenerConteosCamas() {
        log.info("REST request para obtener conteos de camas");

        Map<String, Long> conteos = pacientePorCamaService.obtenerConteosCamas();

        return ResponseEntity.ok(
                ApiResponse.success(conteos, "Conteos obtenidos exitosamente")
        );
    }

    /**
     * Obtiene resumen de ocupación por porcentajes
     * GET /pacientes/estadisticas/porcentajes
     */
    @GetMapping("/estadisticas/porcentajes")
    public ResponseEntity<ApiResponse<Map<String, Double>>> obtenerPorcentajesOcupacion() {
        log.info("REST request para obtener porcentajes de ocupación");

        Map<String, Double> porcentajes = pacientePorCamaService.obtenerPorcentajesOcupacion();

        return ResponseEntity.ok(
                ApiResponse.success(porcentajes, "Porcentajes obtenidos exitosamente")
        );
    }

    /**
     * Busca camas y pacientes con filtros múltiples
     * GET /pacientes/buscar?estado={estado}
     */
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<PacientePorCamaDTO>>> buscarConFiltros(
            @RequestParam Map<String, String> filtros) {
        log.info("REST request para buscar con filtros: {}", filtros);

        // Convertir a Map<String, Object> para el service
        Map<String, Object> filtrosObj = new HashMap<>();
        filtros.forEach((key, value) -> filtrosObj.put(key, value));

        List<PacientePorCamaDTO> resultados = pacientePorCamaService.buscarConFiltros(filtrosObj);

        return ResponseEntity.ok(
                ApiResponse.success(resultados, "Búsqueda realizada exitosamente")
        );
    }

    /**
     * Health Check específico del módulo de pacientes
     * GET /pacientes/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        log.debug("Health check para módulo de pacientes");

        return ResponseEntity.ok(
                ApiResponse.success("OK", "Módulo de pacientes funcionando correctamente")
        );
    }

    /**
     * Información del módulo de pacientes
     * GET /pacientes/info
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInfo() {
        log.debug("Obteniendo información del módulo de pacientes");

        Map<String, Object> info = new HashMap<>();
        info.put("module", "Pacientes por Cama");
        info.put("version", "1.0.0");
        info.put("description", "Gestión de pacientes hospitalizados por cama");
        info.put("endpoints", new String[]{
                "GET /pacientes/camas - Todas las camas",
                "GET /pacientes/camas/ocupadas - Camas ocupadas",
                "GET /pacientes/camas/disponibles - Camas disponibles",
                "GET /pacientes/cama/{bedNumber} - Cama específica",
                "GET /pacientes/camas/rango?start={start}&end={end} - Rango de camas",
                "GET /pacientes/buscar/dni/{dni} - Buscar por DNI",
                "GET /pacientes/buscar/nombre/{nombre} - Buscar por nombre",
                "GET /pacientes/buscar/medico/{medico} - Buscar por médico",
                "GET /pacientes/estadisticas/ocupacion - Estadísticas completas",
                "GET /pacientes/estadisticas/conteos - Conteos por estado",
                "GET /pacientes/estadisticas/porcentajes - Porcentajes de ocupación",
                "GET /pacientes/cama/{bedNumber}/ocupada - Verificar ocupación",
                "GET /pacientes/cama/{bedNumber}/existe - Verificar existencia",
                "GET /pacientes/buscar?estado={estado} - Buscar con filtros",
                "GET /pacientes/health - Health check",
                "GET /pacientes/info - Información del módulo"
        });

        return ResponseEntity.ok(
                ApiResponse.success(info, "Información del módulo")
        );
    }
}