package com.formacionbdi.microservicios.app.notas.controllers;

import com.formacionbdi.microservicios.app.notas.models.dto.HospitalizacionNotaDTO;
import com.formacionbdi.microservicios.commons.response.ApiResponse;
import com.formacionbdi.microservicios.app.notas.services.HospitalizacionNotaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * CONTROLLER para gestión de notas de hospitalización
 * Aplicando programación funcional y código limpio
 */
@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
@Slf4j
public class HospitalizacionNotaController {

    private final HospitalizacionNotaService notaService;

    // ===== VALIDACIÓN CRÍTICA =====

    /**
     * Verifica si un médico puede crear una nueva nota para una hospitalización
     * GET /notas/medico/{medicoId}/hospitalizacion/{hospitalizacionId}/puede-crear
     */
    @GetMapping("/medico/{medicoId}/hospitalizacion/{hospitalizacionId}/puede-crear")
    public ResponseEntity<ApiResponse<Map<String, Object>>> puedeCrearNota(
            @PathVariable @NotNull @Min(1) Long medicoId,
            @PathVariable @NotNull @Min(1) Long hospitalizacionId) {

        log.info(" Validando creación de nota - Médico: {}, Hospitalización: {}",
                medicoId, hospitalizacionId);

        return ejecutarConManejo(() -> {
            boolean puedeCrear = notaService.puedeCrearNota(medicoId, hospitalizacionId);

            Map<String, Object> resultado = Map.of(
                    "puede_crear", puedeCrear,
                    "medico_id", medicoId,
                    "hospitalizacion_id", hospitalizacionId,
                    "razon", puedeCrear ? "Sin restricciones" : "Ya tiene una nota en borrador"
            );

            return ApiResponse.success(resultado,
                    puedeCrear ? "Puede crear nueva nota" : "No puede crear nueva nota");
        });
    }

    // ===== CONSULTAR NOTAS =====

    /**
     * Obtiene todas las notas de una hospitalización
     * GET /notas/hospitalizacion/{hospitalizacionId}
     */
    @GetMapping("/hospitalizacion/{hospitalizacionId}")
    public ResponseEntity<ApiResponse<List<HospitalizacionNotaDTO>>> obtenerNotasPorHospitalizacion(
            @PathVariable @NotNull @Min(1) Long hospitalizacionId,
            @RequestParam(defaultValue = "todas") String estado) {

        log.info(" Obteniendo notas para hospitalización {} con estado: {}",
                hospitalizacionId, estado);

        return ejecutarConManejo(() -> {
            List<HospitalizacionNotaDTO> notas;
            String estadoLower = estado.toLowerCase();

            if ("borrador".equals(estadoLower)) {
                notas = notaService.obtenerNotasBorrador(hospitalizacionId);
            } else if ("finalizada".equals(estadoLower)) {
                notas = notaService.obtenerNotasFinalizadas(hospitalizacionId);
            } else {
                notas = notaService.obtenerNotasPorHospitalizacion(hospitalizacionId);
            }

            return ApiResponse.success(notas,
                    String.format("Notas obtenidas exitosamente (%d encontradas)", notas.size()));
        });
    }

    /**
     * Obtiene una nota específica por ID
     * GET /notas/{notaId}
     */
    @GetMapping("/{notaId}")
    public ResponseEntity<ApiResponse<HospitalizacionNotaDTO>> obtenerNotaPorId(
            @PathVariable @NotNull @Min(1) Long notaId) {

        log.info(" Obteniendo nota con ID: {}", notaId);

        return ejecutarConManejo(() ->
                notaService.obtenerNotaPorId(notaId)
                        .map(nota -> ApiResponse.success(nota, "Nota obtenida exitosamente"))
                        .orElse(ApiResponse.error("Nota no encontrada"))
        );
    }

    /**
     * Busca notas por número de cuenta
     * GET /notas/cuenta/{numeroCuenta}
     */
    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<ApiResponse<List<HospitalizacionNotaDTO>>> buscarPorNumeroCuenta(
            @PathVariable String numeroCuenta) {

        log.info(" Buscando notas por número de cuenta: {}", numeroCuenta);

        return ejecutarConManejo(() -> {
            var notas = notaService.buscarPorNumeroCuenta(numeroCuenta);
            return ApiResponse.success(notas,
                    String.format("Notas encontradas para cuenta %s (%d registros)",
                            numeroCuenta, notas.size()));
        });
    }

    /**
     * Busca notas por tipo en una hospitalización
     * GET /notas/hospitalizacion/{hospitalizacionId}/tipo/{tipoNota}
     */
    @GetMapping("/hospitalizacion/{hospitalizacionId}/tipo/{tipoNota}")
    public ResponseEntity<ApiResponse<List<HospitalizacionNotaDTO>>> buscarPorTipo(
            @PathVariable @NotNull @Min(1) Long hospitalizacionId,
            @PathVariable String tipoNota) {

        log.info(" Buscando notas tipo {} para hospitalización {}", tipoNota, hospitalizacionId);

        return ejecutarConManejo(() -> {
            var notas = notaService.buscarPorTipo(hospitalizacionId, tipoNota);
            String tipoDescripcion = "01".equals(tipoNota) ? "Evolución" : "Interconsulta";

            return ApiResponse.success(notas,
                    String.format("Notas de %s obtenidas (%d encontradas)",
                            tipoDescripcion, notas.size()));
        });
    }

    /**
     * Busca notas de un médico en rango de fechas
     * GET /notas/medico/{medicoId}/fechas
     */
    @GetMapping("/medico/{medicoId}/fechas")
    public ResponseEntity<ApiResponse<List<HospitalizacionNotaDTO>>> buscarPorMedicoYFechas(
            @PathVariable @NotNull @Min(1) Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

        log.info(" Buscando notas del médico {} entre {} y {}",
                medicoId, fechaInicio, fechaFin);

        return ejecutarConManejo(() -> {
            var notas = notaService.buscarPorMedicoYFechas(medicoId, fechaInicio, fechaFin);
            return ApiResponse.success(notas,
                    String.format("Notas del médico obtenidas (%d encontradas)", notas.size()));
        });
    }

    // ===== CRUD INTELIGENTE =====

    /**
     * Crea una nueva nota (con auto-limpieza)
     * POST /notas/crear
     */
    @PostMapping("/crear")
    public ResponseEntity<ApiResponse<HospitalizacionNotaDTO>> crearNota(
            @Valid @RequestBody HospitalizacionNotaDTO notaDTO) {

        log.info(" Creando nueva nota para hospitalización: {}", notaDTO.getHospitalizacionId());
        
        // Establecer fecha de creación
        notaDTO.setCreadoEn(LocalDateTime.now());
        
        var notaCreada = notaService.crearNota(notaDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(notaCreada,
                        String.format("Nota creada exitosamente con ID %d", notaCreada.getId())));
    }

    /**
     * Actualiza una nota existente
     * PUT /notas/{notaId}
     */
    @PutMapping("/{notaId}")
    public ResponseEntity<ApiResponse<HospitalizacionNotaDTO>> actualizarNota(
            @PathVariable @NotNull @Min(1) Long notaId,
            @Valid @RequestBody HospitalizacionNotaDTO notaDTO,
            @RequestParam @NotNull @Min(1) Long medicoId) {

        log.info(" Actualizando nota {} por médico {}", notaId, medicoId);

        return ejecutarConManejo(() -> {
            var notaActualizada = notaService.actualizarNota(notaId, notaDTO, medicoId);
            return ApiResponse.success(notaActualizada, "Nota actualizada exitosamente");
        });
    }

    /**
     * Finaliza una nota (borrador → finalizada)
     * PATCH /notas/{notaId}/finalizar
     */
    @PatchMapping("/{notaId}/finalizar")
    public ResponseEntity<ApiResponse<HospitalizacionNotaDTO>> finalizarNota(
            @PathVariable @NotNull @Min(1) Long notaId,
            @RequestParam @NotNull @Min(1) Long medicoId) {

        log.info(" Finalizando nota {} por médico {}", notaId, medicoId);

        return ejecutarConManejo(() -> {
            var notaFinalizada = notaService.finalizarNota(notaId, medicoId);
            return ApiResponse.success(notaFinalizada, "Nota finalizada exitosamente");
        });
    }

    /**
     * Elimina una nota
     * DELETE /notas/{notaId}
     */
    @DeleteMapping("/{notaId}")
    public ResponseEntity<ApiResponse<Void>> eliminarNota(
            @PathVariable @NotNull @Min(1) Long notaId,
            @RequestParam @NotNull @Min(1) Long medicoId) {

        log.info(" Eliminando nota {} por médico {}", notaId, medicoId);

        return ejecutarConManejo(() -> {
            notaService.eliminarNota(notaId, medicoId);
            return ApiResponse.success("Nota eliminada exitosamente");
        }, HttpStatus.NO_CONTENT);
    }

    // ===== GESTIÓN DE AUDIO =====

    /**
     * Elimina el audio de una nota específica
     * DELETE /notas/{notaId}/audio
     */
    @DeleteMapping("/{notaId}/audio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eliminarAudioNota(
            @PathVariable @NotNull @Min(1) Long notaId,
            @RequestParam @NotNull @Min(1) Long medicoId) {

        log.info(" Eliminando audio de nota {} por médico {}", notaId, medicoId);

        return ejecutarConManejo(() -> {
            boolean eliminado = notaService.eliminarAudioNota(notaId, medicoId);

            Map<String, Object> resultado = Map.of(
                    "nota_id", notaId,
                    "audio_eliminado", eliminado,
                    "mensaje", eliminado ? "Audio eliminado exitosamente" : "La nota no tenía audio"
            );

            return ApiResponse.success(resultado,
                    eliminado ? "Audio eliminado" : "No había audio para eliminar");
        });
    }

    /**
     * Verifica si una nota tiene audio disponible
     * GET /notas/{notaId}/audio/disponible
     */
    @GetMapping("/{notaId}/audio/disponible")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verificarAudioDisponible(
            @PathVariable @NotNull @Min(1) Long notaId) {

        return ejecutarConManejo(() -> {
            boolean tieneAudio = notaService.tieneAudioDisponible(notaId);

            Map<String, Object> resultado = Map.of(
                    "nota_id", notaId,
                    "tiene_audio", tieneAudio
            );

            return ApiResponse.success(resultado,
                    tieneAudio ? "Audio disponible" : "Sin audio disponible");
        });
    }

    // ===== ESTADÍSTICAS =====

    /**
     * Obtiene estadísticas de una hospitalización
     * GET /notas/hospitalizacion/{hospitalizacionId}/estadisticas
     */
    @GetMapping("/hospitalizacion/{hospitalizacionId}/estadisticas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadisticas(
            @PathVariable @NotNull @Min(1) Long hospitalizacionId) {

        log.info(" Obteniendo estadísticas de hospitalización {}", hospitalizacionId);

        return ejecutarConManejo(() -> {
            var estadisticas = notaService.obtenerEstadisticasHospitalizacion(hospitalizacionId);
            return ApiResponse.success(estadisticas, "Estadísticas obtenidas exitosamente");
        });
    }

    // ===== GENERACIÓN DE PDF =====

    /**
     * Genera PDF de una nota específica
     * GET /notas/{notaId}/pdf
     */
    @GetMapping("/{notaId}/pdf")
    public ResponseEntity<byte[]> generarPdfNota(@PathVariable @NotNull @Min(1) Long notaId) {
        log.info(" Generando PDF de nota {}", notaId);

        try {
            byte[] pdfBytes = notaService.generarPdfNota(notaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    String.format("nota_%d.pdf", notaId));
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error(" Error generando PDF de nota {}: {}", notaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Genera PDF consolidado de una hospitalización
     * GET /notas/hospitalizacion/{hospitalizacionId}/pdf
     */
    @GetMapping("/hospitalizacion/{hospitalizacionId}/pdf")
    public ResponseEntity<byte[]> generarPdfConsolidado(
            @PathVariable @NotNull @Min(1) Long hospitalizacionId) {

        log.info(" Generando PDF consolidado de hospitalización {}", hospitalizacionId);

        try {
            byte[] pdfBytes = notaService.generarPdfConsolidado(hospitalizacionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    String.format("notas_hospitalizacion_%d.pdf", hospitalizacionId));
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error(" Error generando PDF consolidado de hospitalización {}: {}",
                    hospitalizacionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== UTILIDADES =====

    /**
     * Health Check del microservicio
     * GET /notas/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "microservicio", "notas-vitales",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now(),
                "endpoints_disponibles", List.of(
                        "GET /notas/medico/{medicoId}/hospitalizacion/{hospitalizacionId}/puede-crear",
                        "GET /notas/hospitalizacion/{hospitalizacionId}",
                        "GET /notas/{notaId}",
                        "POST /notas/crear",
                        "PUT /notas/{notaId}",
                        "DELETE /notas/{notaId}",
                        "GET /notas/{notaId}/pdf"
                )
        );

        return ResponseEntity.ok(ApiResponse.success(health, "Microservicio de notas operativo"));
    }

    /**
     * Información del microservicio
     * GET /notas/info
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> infoMicroservicio() {
        Map<String, Object> info = Map.of(
                "nombre", "Microservicio de Notas Vitales",
                "descripcion", "Gestión completa de notas de hospitalización con audio, firma digital y PDF",
                "version", "1.0.0",
                "puerto", 8004,
                "caracteristicas", List.of(
                        " Validación de reglas de negocio",
                        " CRUD inteligente con auto-limpieza",
                        " Gestión de audio y transcripciones",
                        " Firma digital integrada",
                        " Generación de PDF individual y consolidado",
                        " Estadísticas y reportes",
                        " Auto-limpieza de archivos antiguos"
                )
        );

        return ResponseEntity.ok(ApiResponse.success(info, "Información del microservicio"));
    }

    // ===== MÉTODO HELPER FUNCIONAL =====

    private <T> ResponseEntity<ApiResponse<T>> ejecutarConManejo(Supplier<ApiResponse<T>> operacion) {
        return ejecutarConManejo(operacion, HttpStatus.OK);
    }

    private <T> ResponseEntity<ApiResponse<T>> ejecutarConManejo(
            Supplier<ApiResponse<T>> operacion,
            HttpStatus statusExito) {
        try {
            var resultado = operacion.get();
            return ResponseEntity.status(statusExito).body(resultado);
        } catch (Exception e) {
            log.error(" Error en operación: {}", e.getMessage(), e);
            throw e; // Re-lanzar para que lo maneje GlobalExceptionHandler
        }
    }
}