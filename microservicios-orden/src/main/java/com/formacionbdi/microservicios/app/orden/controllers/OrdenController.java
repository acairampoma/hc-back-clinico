package com.formacionbdi.microservicios.app.orden.controllers;

import com.formacionbdi.microservicios.app.orden.models.dto.*;
import com.formacionbdi.microservicios.app.orden.models.response.ApiResponse;
import com.formacionbdi.microservicios.app.orden.services.OrdenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 🩺 CONTROLLER ÓRDENES MÉDICAS MEJORADO
 * Puerto: 8006 - Microservicio Órdenes
 * ✅ Con @RequestMapping base para URLs más limpias
 */
@RestController
@RequestMapping("/ordenes")  // ✅ AGREGADO: Base path
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrdenController {

    private final OrdenService ordenService;

    // =====================================================
    // 📋 CRUD PRINCIPALES
    // =====================================================

    /**
     * Crear nueva orden médica
     * POST /ordenes/crear
     */
    @PostMapping("/crear")  // ✅ SIMPLIFICADO: era "/ordenes/crear"
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> crearOrden(
            @Valid @RequestBody OrdenCabDTO ordenDTO) {

        log.info("🔥 REST - Crear orden médica: Tipo={}, Origen={}:{}",
                ordenDTO.getTipoOrden(), ordenDTO.getTipoOrigen(), ordenDTO.getOrigenId());

        try {
            OrdenCompletaDTO ordenCreada = ordenService.crearOrden(ordenDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(ordenCreada,
                            "Orden médica creada exitosamente: " + ordenCreada.getNumeroOrden()));

        } catch (Exception e) {
            log.error("❌ Error al crear orden: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al crear orden: " + e.getMessage()));
        }
    }

    /**
     * Obtener orden completa por ID
     * GET /ordenes/{id}
     */
    @GetMapping("/{id}")  // ✅ SIMPLIFICADO: era "/ordenes/{id}"
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
    @GetMapping("/numero/{numeroOrden}")  // ✅ SIMPLIFICADO
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
    @PutMapping("/{id}")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> actualizarOrden(
            @PathVariable Long id,
            @RequestParam Long medicoId,
            @Valid @RequestBody ActualizarOrdenDTO actualizarDTO) {

        log.info("📝 REST - Actualizar orden ID: {} por médico: {}", id, medicoId);

        try {
            OrdenCompletaDTO ordenActualizada = ordenService.actualizarOrden(id, actualizarDTO, medicoId);

            return ResponseEntity.ok(
                    ApiResponse.success(ordenActualizada, "Orden actualizada exitosamente"));

        } catch (Exception e) {
            log.error("❌ Error al actualizar orden {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al actualizar orden: " + e.getMessage()));
        }
    }

    /**
     * Cambiar estado de orden
     * PATCH /ordenes/{id}/estado
     */
    @PatchMapping("/{id}/estado")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> cambiarEstadoOrden(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoOrdenDTO estadoDTO) {

        log.info("🔄 REST - Cambiar estado orden ID: {} a estado: {}", id, estadoDTO.getEstado());

        try {
            OrdenCompletaDTO ordenActualizada = ordenService.cambiarEstadoOrden(id, estadoDTO);

            return ResponseEntity.ok(
                    ApiResponse.success(ordenActualizada, "Estado cambiado exitosamente"));

        } catch (Exception e) {
            log.error("❌ Error al cambiar estado orden {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al cambiar estado: " + e.getMessage()));
        }
    }

    /**
     * Eliminar orden (lógica)
     * DELETE /ordenes/{id}
     */
    @DeleteMapping("/{id}")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<String>> eliminarOrden(
            @PathVariable Long id,
            @RequestParam Long medicoId) {

        log.info("🗑️ REST - Eliminar orden ID: {} por médico: {}", id, medicoId);

        try {
            ordenService.eliminarOrden(id, medicoId);

            return ResponseEntity.ok(
                    ApiResponse.success("Orden ID: " + id, "Orden eliminada exitosamente"));

        } catch (Exception e) {
            log.error("❌ Error al eliminar orden {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al eliminar orden: " + e.getMessage()));
        }
    }

    // =====================================================
    // 🔍 CONSULTAS Y FILTROS
    // =====================================================

    /**
     * Listar todas las órdenes
     * GET /ordenes
     */
    @GetMapping  // ✅ SIMPLIFICADO: era "/ordenes"
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
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al obtener órdenes: " + e.getMessage()));
        }
    }

    /**
     * Obtener órdenes por paciente
     * GET /ordenes/paciente/{pacienteId}
     */
    @GetMapping("/paciente/{pacienteId}")  // ✅ SIMPLIFICADO
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
    @GetMapping("/medico/{medicoId}")  // ✅ SIMPLIFICADO
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
    @GetMapping("/estado/{estado}")  // ✅ SIMPLIFICADO
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
    @GetMapping("/prioridad/{prioridad}")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<List<OrdenResumenDTO>>> obtenerOrdenesPorPrioridad(
            @PathVariable String prioridad) {

        log.info("⚡ REST - Obtener órdenes prioridad: {}", prioridad);

        List<OrdenResumenDTO> ordenes = ordenService.obtenerOrdenesPorPrioridad(prioridad);

        return ResponseEntity.ok(
                ApiResponse.success(ordenes, "Órdenes por prioridad obtenidas"));
    }

    // =====================================================
    // 🔬 GESTIÓN DE EXÁMENES
    // =====================================================

    /**
     * Obtener exámenes de una orden
     * GET /ordenes/{id}/examenes
     */
    @GetMapping("/{id}/examenes")  // ✅ SIMPLIFICADO
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
    @PostMapping("/{id}/examenes")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<OrdenCompletaDTO>> agregarExamenAOrden(
            @PathVariable Long id,
            @RequestParam Long medicoId,
            @Valid @RequestBody OrdenDetDTO examenDTO) {

        log.info("➕ REST - Agregar examen {} a orden: {}", examenDTO.getExamenId(), id);

        try {
            OrdenCompletaDTO ordenActualizada = ordenService.agregarExamenAOrden(id, examenDTO, medicoId);

            return ResponseEntity.ok(
                    ApiResponse.success(ordenActualizada, "Examen agregado exitosamente"));

        } catch (Exception e) {
            log.error("❌ Error al agregar examen: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al agregar examen: " + e.getMessage()));
        }
    }

    // =====================================================
    // 📊 ESTADÍSTICAS Y REPORTES
    // =====================================================

    /**
     * Estadísticas por estado
     * GET /ordenes/estadisticas/estados
     */
    @GetMapping("/estadisticas/estados")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<List<OrdenService.EstadisticaDTO>>> obtenerEstadisticasEstados() {

        log.info("📊 REST - Estadísticas por estados");

        List<OrdenService.EstadisticaDTO> estadisticas = ordenService.obtenerEstadisticasPorEstado();

        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Estadísticas por estado obtenidas"));
    }

    /**
     * Exámenes más solicitados
     * GET /ordenes/estadisticas/examenes-populares
     */
    @GetMapping("/estadisticas/examenes-populares")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<List<OrdenService.ExamenEstadisticaDTO>>> obtenerExamenesPopulares() {

        log.info("🏆 REST - Exámenes más solicitados");

        List<OrdenService.ExamenEstadisticaDTO> estadisticas = ordenService.obtenerExamenesMasSolicitados();

        return ResponseEntity.ok(
                ApiResponse.success(estadisticas, "Exámenes más solicitados obtenidos"));
    }

    // =====================================================
    // ✅ UTILIDADES Y VALIDACIONES
    // =====================================================

    /**
     * Health Check del microservicio
     * GET /ordenes/health
     */
    @GetMapping("/health")  // ✅ SIMPLIFICADO
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
    @GetMapping("/info")  // ✅ SIMPLIFICADO
    public ResponseEntity<ApiResponse<String[]>> infoMicroservicio() {

        log.info("ℹ️ REST - Info microservicio órdenes");

        String[] info = {
                "🩺 Microservicio Órdenes Médicas",
                "📋 Puerto: 8006",
                "🔬 Endpoints: 18+ rutas disponibles",
                "⚡ Estado: Operativo",
                "🏥 Funcionalidad: Gestión completa de órdenes médicas",
                "💊 Tipos: LAB, IMG, PROC, FUNC",
                "📊 Estadísticas: Estados, tipos, prioridades",
                "🔍 Consultas: Por paciente, médico, origen, fecha",
                "🔄 CRUD: Crear, consultar, actualizar, eliminar",
                "🔗 JOIN: Integración con tabla examenes"
        };

        return ResponseEntity.ok(
                ApiResponse.success(info, "Información del microservicio"));
    }
}