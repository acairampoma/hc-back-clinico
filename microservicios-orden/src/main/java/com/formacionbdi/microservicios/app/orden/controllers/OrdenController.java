package com.formacionbdi.microservicios.app.orden.controllers;

import com.formacionbdi.microservicios.app.orden.models.dto.*;
import com.formacionbdi.microservicios.commons.response.ApiResponse;
import com.formacionbdi.microservicios.app.orden.services.OrdenService;
import com.formacionbdi.microservicios.app.orden.exception.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 🩺 CONTROLLER ÓRDENES MÉDICAS - Java 17 Completo
 * Puerto: 8006 - Microservicio Órdenes
 *
 * ✅ Características Java 17:
 * - Switch expressions para manejo de errores
 * - Text blocks para respuestas
 * - Pattern matching para validaciones
 * - Records para DTOs
 * - Métodos compatibles con OrdenService refactorizado
 */
@RestController
@RequestMapping("/ordenes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrdenController {

    private final OrdenService ordenService;

    // =====================================================
    // 📋 CRUD PRINCIPALES CON JAVA 17
    // =====================================================

    /**
     * Crear nueva orden médica
     * POST /ordenes
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> crearOrden(@Valid @RequestBody OrdenCabDTO ordenDTO) {
        log.info("🔥 REST - Crear orden médica: Tipo={}, Origen={}:{}",
                ordenDTO.tipoOrden(), ordenDTO.tipoOrigen(), ordenDTO.origenId());

        try {
            OrdenCompletaDTO ordenCreada = ordenService.crearOrden(ordenDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(ordenCreada,
                            "Orden médica creada exitosamente: " + ordenCreada.numeroOrden()));

        } catch (OrdenBusinessException e) {
            log.error("❌ Error de negocio al crear orden: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al crear orden: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno al crear orden"));
        }
    }

    /**
     * Obtener orden completa por ID
     * GET /ordenes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> obtenerOrden(@PathVariable Long id) {
        log.info("🔍 REST - Obtener orden ID: {}", id);

        return ordenService.obtenerOrdenCompleta(id)
                .map(orden -> ResponseEntity.ok(
                        ApiResponse.success(orden, "Orden encontrada")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Orden no encontrada con ID: " + id)));
    }

    /**
     * Obtener orden por número
     * GET /ordenes/numero/{numeroOrden}
     */
    @GetMapping("/numero/{numeroOrden}")
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> obtenerOrdenPorNumero(
            @PathVariable String numeroOrden) {
        log.info("🔍 REST - Obtener orden número: {}", numeroOrden);

        return ordenService.obtenerOrdenPorNumero(numeroOrden)
                .map(orden -> ResponseEntity.ok(
                        ApiResponse.success(orden, "Orden encontrada")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Orden no encontrada: " + numeroOrden)));
    }

    /**
     * Actualizar orden existente
     * PUT /ordenes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> actualizarOrden(
            @PathVariable Long id,
            @RequestParam Long medicoId,
            @Valid @RequestBody ActualizarOrdenDTO actualizarDTO) {

        log.info("📝 REST - Actualizar orden ID: {} por médico: {}", id, medicoId);

        try {
            OrdenCompletaDTO ordenActualizada = ordenService.actualizarOrden(id, actualizarDTO, medicoId);
            return ResponseEntity.ok(
                    ApiResponse.success(ordenActualizada, "Orden actualizada exitosamente"));

        } catch (OrdenNotFoundException e) {
            log.error("❌ Orden no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (OrdenBusinessException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al actualizar orden: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno al actualizar orden"));
        }
    }

    /**
     * Cambiar estado de orden
     * PATCH /ordenes/{id}/estado
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> cambiarEstadoOrden(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoOrdenDTO estadoDTO) {

        log.info("🔄 REST - Cambiar estado orden ID: {} a estado: {}", id, estadoDTO.nuevoEstado());

        try {
            OrdenCompletaDTO ordenActualizada = ordenService.cambiarEstadoOrden(id, estadoDTO);

            // ✅ CORREGIDO: Usar solo el código de estado
            String descripcionEstado = obtenerDescripcionEstado(estadoDTO.nuevoEstado());

            return ResponseEntity.ok(
                    ApiResponse.success(ordenActualizada,
                            "Estado cambiado exitosamente a: " + descripcionEstado));

        } catch (OrdenNotFoundException e) {
            log.error("❌ Orden no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (OrdenBusinessException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al cambiar estado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno al cambiar estado"));
        }
    }

    /**
     * Eliminar orden (lógica)
     * DELETE /ordenes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> eliminarOrden(
            @PathVariable Long id,
            @RequestParam Long medicoId) {

        log.info("🗑️ REST - Eliminar orden ID: {} por médico: {}", id, medicoId);

        try {
            ordenService.eliminarOrden(id, medicoId);
            return ResponseEntity.ok(
                    ApiResponse.success("Orden ID: " + id, "Orden eliminada exitosamente"));

        } catch (OrdenNotFoundException e) {
            log.error("❌ Orden no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (OrdenBusinessException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al eliminar orden: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno al eliminar orden"));
        }
    }

    // =====================================================
    // 🔍 CONSULTAS Y LISTADOS
    // =====================================================

    /**
     * Listar todas las órdenes
     * GET /ordenes
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerTodasLasOrdenes(
            @RequestParam(required = false) String tipoOrigen,
            @RequestParam(required = false) Long origenId) {

        log.info("📋 REST - Obtener órdenes - Origen: {}:{}", tipoOrigen, origenId);

        try {
            List<OrdenResumenDTO> ordenes;

            if (tipoOrigen != null && origenId != null) {
                ordenes = ordenService.obtenerOrdenesPorOrigen(tipoOrigen, origenId);
            } else {
                ordenes = ordenService.obtenerTodasLasOrdenes();
            }

            return ResponseEntity.ok(
                    ApiResponse.success(ordenes, "Órdenes obtenidas exitosamente"));

        } catch (Exception e) {
            log.error("❌ Error al obtener órdenes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener órdenes"));
        }
    }

    /**
     * Obtener órdenes por paciente
     * GET /ordenes/paciente/{pacienteId}
     */
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesPorPaciente(
            @PathVariable Long pacienteId) {

        log.info("👤 REST - Obtener órdenes paciente: {}", pacienteId);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesPorPaciente(pacienteId);
        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes del paciente obtenidas"));
    }

    /**
     * Obtener órdenes por médico
     * GET /ordenes/medico/{medicoId}
     */
    @GetMapping("/medico/{medicoId}")
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesPorMedico(
            @PathVariable Long medicoId) {

        log.info("👨‍⚕️ REST - Obtener órdenes médico: {}", medicoId);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesPorMedico(medicoId);
        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes del médico obtenidas"));
    }

    /**
     * Obtener órdenes por estado
     * GET /ordenes/estado/{estado}
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesPorEstado(
            @PathVariable String estado) {

        log.info("📊 REST - Obtener órdenes estado: {}", estado);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesPorEstado(estado);
        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes por estado obtenidas"));
    }

    /**
     * Obtener órdenes por prioridad
     * GET /ordenes/prioridad/{prioridad}
     */
    @GetMapping("/prioridad/{prioridad}")
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesPorPrioridad(
            @PathVariable String prioridad) {

        log.info("⚡ REST - Obtener órdenes prioridad: {}", prioridad);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesPorPrioridad(prioridad);
        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes por prioridad obtenidas"));
    }

    /**
     * Obtener órdenes por tipo
     * GET /ordenes/tipo/{tipoOrden}
     */
    @GetMapping("/tipo/{tipoOrden}")
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesPorTipo(
            @PathVariable String tipoOrden) {

        log.info("📝 REST - Obtener órdenes tipo: {}", tipoOrden);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesPorTipo(tipoOrden);
        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes por tipo obtenidas"));
    }

    /**
     * Obtener órdenes por fecha
     * GET /ordenes/fecha/{fecha}
     */
    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        log.info("📅 REST - Obtener órdenes fecha: {}", fecha);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesPorFecha(fecha);
        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes por fecha obtenidas"));
    }

    /**
     * Obtener órdenes programadas
     * GET /ordenes/programadas/{fecha}
     */
    @GetMapping("/programadas/{fecha}")
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesProgramadas(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        log.info("⏰ REST - Obtener órdenes programadas: {}", fecha);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesProgramadas(fecha);
        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes programadas obtenidas"));
    }

    // =====================================================
    // 🔬 GESTIÓN DE EXÁMENES
    // =====================================================

    /**
     * Obtener exámenes de una orden
     * GET /ordenes/{id}/examenes
     */
    @GetMapping("/{id}/examenes")
    public ResponseEntity<ApiResponse<List<OrdenExamenDTO>>> obtenerExamenesDeOrden(
            @PathVariable Long id) {

        log.info("🔬 REST - Obtener exámenes orden: {}", id);

        List<OrdenExamenDTO> examenes = ordenService.obtenerExamenesDeOrden(id);
        return ResponseEntity.ok(
                ApiResponse.success(examenes, "Exámenes de la orden obtenidos"));
    }

    /**
     * Agregar examen a orden
     * POST /ordenes/{id}/examenes
     */
    @PostMapping("/{id}/examenes")
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> agregarExamenAOrden(
            @PathVariable Long id,
            @RequestParam Long medicoId,
            @Valid @RequestBody OrdenDetDTO examenDTO) {

        log.info("➕ REST - Agregar examen {} a orden: {}", examenDTO.examenId(), id);

        try {
            OrdenCompletaDTO ordenActualizada = ordenService.agregarExamenAOrden(id, examenDTO, medicoId);
            return ResponseEntity.ok(
                    ApiResponse.success(ordenActualizada, "Examen agregado exitosamente"));

        } catch (OrdenNotFoundException e) {
            log.error("❌ Orden no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (OrdenBusinessException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al agregar examen: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno al agregar examen"));
        }
    }

    /**
     * Actualizar examen en orden
     * PUT /ordenes/{ordenId}/examenes/{examenDetalleId}
     */
    @PutMapping("/{ordenId}/examenes/{examenDetalleId}")
    public ResponseEntity<ApiResponse<OrdenExamenDTO>> actualizarExamenEnOrden(
            @PathVariable Long ordenId,
            @PathVariable Long examenDetalleId,
            @RequestParam Long medicoId,
            @Valid @RequestBody ActualizarOrdenDTO.ModificarExamenDTO examenDTO) {

        log.info("✏️ REST - Actualizar examen {} en orden: {}", examenDetalleId, ordenId);

        try {
            OrdenExamenDTO examenActualizado = ordenService.actualizarExamenEnOrden(
                    ordenId, examenDetalleId, examenDTO, medicoId);
            return ResponseEntity.ok(
                    ApiResponse.success(examenActualizado, "Examen actualizado exitosamente"));

        } catch (ExamenNotFoundException e) {
            log.error("❌ Examen no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (OrdenBusinessException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al actualizar examen: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno al actualizar examen"));
        }
    }

    /**
     * Eliminar examen de orden
     * DELETE /ordenes/{ordenId}/examenes/{examenDetalleId}
     */
    @DeleteMapping("/{ordenId}/examenes/{examenDetalleId}")
    public ResponseEntity<ApiResponse<String>> eliminarExamenDeOrden(
            @PathVariable Long ordenId,
            @PathVariable Long examenDetalleId,
            @RequestParam Long medicoId) {

        log.info("🗑️ REST - Eliminar examen {} de orden: {}", examenDetalleId, ordenId);

        try {
            ordenService.eliminarExamenDeOrden(ordenId, examenDetalleId, medicoId);
            return ResponseEntity.ok(
                    ApiResponse.success("Examen eliminado", "Examen eliminado exitosamente de la orden"));

        } catch (ExamenNotFoundException e) {
            log.error("❌ Examen no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al eliminar examen: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno al eliminar examen"));
        }
    }

    // =====================================================
    // 📊 ESTADÍSTICAS Y REPORTES
    // =====================================================

    /**
     * Estadísticas por estado
     * GET /ordenes/estadisticas/estados
     */
    @GetMapping("/estadisticas/estados")
    public ResponseEntity<ApiResponse<List<OrdenService.EstadisticaDTO>>> obtenerEstadisticasEstados() {
        log.info("📊 REST - Estadísticas por estados");

        List<OrdenService.EstadisticaDTO> estadisticas = ordenService.obtenerEstadisticasPorEstado();
        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Estadísticas por estado obtenidas"));
    }

    /**
     * Estadísticas por tipo
     * GET /ordenes/estadisticas/tipos
     */
    @GetMapping("/estadisticas/tipos")
    public ResponseEntity<ApiResponse<List<OrdenService.EstadisticaDTO>>> obtenerEstadisticasTipos() {
        log.info("📋 REST - Estadísticas por tipos");

        List<OrdenService.EstadisticaDTO> estadisticas = ordenService.obtenerEstadisticasPorTipo();
        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Estadísticas por tipo obtenidas"));
    }

    /**
     * Estadísticas por prioridad
     * GET /ordenes/estadisticas/prioridades
     */
    @GetMapping("/estadisticas/prioridades")
    public ResponseEntity<ApiResponse<List<OrdenService.EstadisticaDTO>>> obtenerEstadisticasPrioridades() {
        log.info("⚡ REST - Estadísticas por prioridades");

        List<OrdenService.EstadisticaDTO> estadisticas = ordenService.obtenerEstadisticasPorPrioridad();
        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Estadísticas por prioridad obtenidas"));
    }

    /**
     * Exámenes más solicitados
     * GET /ordenes/estadisticas/examenes-populares
     */
    @GetMapping("/estadisticas/examenes-populares")
    public ResponseEntity<ApiResponse<List<OrdenService.ExamenEstadisticaDTO>>> obtenerExamenesPopulares() {
        log.info("🏆 REST - Exámenes más solicitados");

        List<OrdenService.ExamenEstadisticaDTO> estadisticas = ordenService.obtenerExamenesMasSolicitados();
        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Exámenes más solicitados obtenidos"));
    }

    /**
     * Estadísticas por categoría
     * GET /ordenes/estadisticas/categorias
     */
    @GetMapping("/estadisticas/categorias")
    public ResponseEntity<ApiResponse<List<OrdenService.EstadisticaDTO>>> obtenerEstadisticasCategorias() {
        log.info("🏷️ REST - Estadísticas por categorías");

        List<OrdenService.EstadisticaDTO> estadisticas = ordenService.obtenerEstadisticasPorCategoria();
        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Estadísticas por categoría obtenidas"));
    }

    // =====================================================
    // ✅ UTILIDADES Y VALIDACIONES
    // =====================================================

    /**
     * Verificar si existe orden
     * GET /ordenes/{id}/existe
     */
    @GetMapping("/{id}/existe")
    public ResponseEntity<ApiResponse<Boolean>> existeOrden(@PathVariable Long id) {
        log.info("🔍 REST - Verificar existencia orden: {}", id);

        boolean existe = ordenService.existeOrden(id);
        return ResponseEntity.ok(
                ApiResponse.success(existe, existe ? "Orden existe" : "Orden no existe"));
    }

    /**
     * Verificar si existe orden por número
     * GET /ordenes/numero/{numeroOrden}/existe
     */
    @GetMapping("/numero/{numeroOrden}/existe")
    public ResponseEntity<ApiResponse<Boolean>> existeOrdenPorNumero(@PathVariable String numeroOrden) {
        log.info("🔍 REST - Verificar existencia orden número: {}", numeroOrden);

        boolean existe = ordenService.existeOrdenPorNumero(numeroOrden);
        return ResponseEntity.ok(
                ApiResponse.success(existe, existe ? "Orden existe" : "Orden no existe"));
    }

    /**
     * Generar número de orden
     * GET /ordenes/generar-numero
     */
    @GetMapping("/generar-numero")
    public ResponseEntity<ApiResponse<String>> generarNumeroOrden() {
        log.info("🔢 REST - Generar número de orden");

        String numeroOrden = ordenService.generarNumeroOrden();
        return ResponseEntity.ok(
                ApiResponse.success(numeroOrden, "Número de orden generado"));
    }

    /**
     * Health Check del microservicio
     * GET /ordenes/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        log.info("💚 REST - Health check microservicio órdenes");

        return ResponseEntity.ok(
                ApiResponse.success("Puerto: 8006 - Version: 1.0.0",
                        "Microservicio órdenes funcionando correctamente"));
    }

    /**
     * Info del microservicio
     * GET /ordenes/info
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> infoMicroservicio() {
        log.info("ℹ️ REST - Info microservicio órdenes");

        Map<String, Object> info = new HashMap<>();
        info.put("nombre", "🩺 Microservicio Órdenes Médicas");
        info.put("version", "1.0.0");
        info.put("puerto", 8006);
        info.put("java", "17");
        info.put("framework", "Spring Boot 3.1.0");
        info.put("descripcion", "Gestión completa de órdenes médicas con exámenes");
        info.put("funcionalidades", List.of(
                "CRUD completo de órdenes",
                "Gestión de exámenes",
                "Estadísticas avanzadas",
                "Búsquedas inteligentes",
                "Validaciones de negocio",
                "Operaciones atómicas"
        ));
        info.put("endpoints", 25);
        info.put("estado", "Operativo");

        return ResponseEntity.ok(
                ApiResponse.success(info, "Información del microservicio"));
    }

    // =====================================================
    // 🛠️ MÉTODOS HELPER PRIVADOS
    // =====================================================

    /**
     * Obtener descripción de estado con switch expression (Java 17)
     */
    private String obtenerDescripcionEstado(String estado) {
        return switch (estado != null ? estado : "01") {
            case "01" -> "Solicitada";
            case "02" -> "Programada";
            case "03" -> "En Proceso";
            case "04" -> "Completada";
            case "05" -> "Cancelada";
            default -> "Estado: " + estado;
        };
    }
}