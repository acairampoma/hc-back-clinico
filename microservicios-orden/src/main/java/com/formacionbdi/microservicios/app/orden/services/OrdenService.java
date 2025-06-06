package com.formacionbdi.microservicios.app.orden.services;

import com.formacionbdi.microservicios.app.orden.models.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 🩺 SERVICE INTERFACE ÓRDENES MÉDICAS
 * Puerto: 8006
 */
public interface OrdenService {

    // =====================================================
    // 📋 OPERACIONES CRUD PRINCIPALES
    // =====================================================

    /**
     * Crear nueva orden médica
     * POST /ordenes/crear
     */
    OrdenCompletaDTO crearOrden(OrdenCabDTO ordenDTO);

    /**
     * Obtener orden completa por ID (con JOIN mágico)
     * GET /ordenes/{id}
     */
    Optional<OrdenCompletaDTO> obtenerOrdenCompleta(Long ordenId);

    /**
     * Obtener orden por número
     * GET /ordenes/numero/{numeroOrden}
     */
    Optional<OrdenCompletaDTO> obtenerOrdenPorNumero(String numeroOrden);

    /**
     * Actualizar orden existente
     * PUT /ordenes/{id}
     */
    OrdenCompletaDTO actualizarOrden(Long ordenId, ActualizarOrdenDTO actualizarDTO, Long medicoId);

    /**
     * Cambiar estado de orden
     * PATCH /ordenes/{id}/estado
     */
    OrdenCompletaDTO cambiarEstadoOrden(Long ordenId, ActualizarEstadoOrdenDTO estadoDTO);

    /**
     * Eliminar orden (lógica)
     * DELETE /ordenes/{id}
     */
    void eliminarOrden(Long ordenId, Long medicoId);

    // =====================================================
    // 🔍 CONSULTAS Y FILTROS
    // =====================================================

    /**
     * Listar todas las órdenes activas
     * GET /ordenes
     */
    List<OrdenResumenDTO> obtenerTodasLasOrdenes();

    /**
     * Obtener órdenes por paciente
     * GET /ordenes/paciente/{pacienteId}
     */
    List<OrdenResumenDTO> obtenerOrdenesPorPaciente(Long pacienteId);

    /**
     * Obtener órdenes por médico
     * GET /ordenes/medico/{medicoId}
     */
    List<OrdenResumenDTO> obtenerOrdenesPorMedico(Long medicoId);

    /**
     * Obtener órdenes por origen (HOS, AMB, EMR)
     * GET /ordenes?tipo_origen=HOS&origen_id=123
     */
    List<OrdenResumenDTO> obtenerOrdenesPorOrigen(String tipoOrigen, Long origenId);

    /**
     * Obtener órdenes por estado
     * GET /ordenes/estado/{estado}
     */
    List<OrdenResumenDTO> obtenerOrdenesPorEstado(String estado);

    /**
     * Obtener órdenes por prioridad
     * GET /ordenes/prioridad/{prioridad}
     */
    List<OrdenResumenDTO> obtenerOrdenesPorPrioridad(String prioridad);

    /**
     * Obtener órdenes por tipo
     * GET /ordenes/tipo/{tipoOrden}
     */
    List<OrdenResumenDTO> obtenerOrdenesPorTipo(String tipoOrden);

    /**
     * Obtener órdenes por fecha
     * GET /ordenes/fecha/{fecha}
     */
    List<OrdenResumenDTO> obtenerOrdenesPorFecha(LocalDate fecha);

    /**
     * Obtener órdenes programadas para una fecha
     * GET /ordenes/programadas/{fecha}
     */
    List<OrdenResumenDTO> obtenerOrdenesProgramadas(LocalDate fecha);

    // =====================================================
    // 🔬 GESTIÓN DE EXÁMENES
    // =====================================================

    /**
     * Obtener exámenes de una orden (con JOIN mágico)
     * GET /ordenes/{id}/examenes
     */
    List<OrdenExamenDTO> obtenerExamenesDeOrden(Long ordenId);

    /**
     * Agregar examen a orden existente
     * POST /ordenes/{id}/examenes
     */
    OrdenCompletaDTO agregarExamenAOrden(Long ordenId, OrdenDetDTO examenDTO, Long medicoId);

    /**
     * Actualizar examen específico
     * PUT /ordenes/{ordenId}/examenes/{examenId}
     */
    OrdenExamenDTO actualizarExamenEnOrden(Long ordenId, Long examenDetalleId,
                                           ActualizarOrdenDTO.ModificarExamenDTO examenDTO, Long medicoId);

    /**
     * Eliminar examen de orden
     * DELETE /ordenes/{ordenId}/examenes/{examenId}
     */
    void eliminarExamenDeOrden(Long ordenId, Long examenDetalleId, Long medicoId);

    // =====================================================
    // 📊 ESTADÍSTICAS Y REPORTES
    // =====================================================

    /**
     * Estadísticas por estado
     * GET /ordenes/estadisticas/estados
     */
    List<EstadisticaDTO> obtenerEstadisticasPorEstado();

    /**
     * Estadísticas por tipo de orden
     * GET /ordenes/estadisticas/tipos
     */
    List<EstadisticaDTO> obtenerEstadisticasPorTipo();

    /**
     * Estadísticas por prioridad
     * GET /ordenes/estadisticas/prioridades
     */
    List<EstadisticaDTO> obtenerEstadisticasPorPrioridad();

    /**
     * Exámenes más solicitados
     * GET /ordenes/estadisticas/examenes-populares
     */
    List<ExamenEstadisticaDTO> obtenerExamenesMasSolicitados();

    /**
     * Estadísticas por categoría de examen
     * GET /ordenes/estadisticas/categorias
     */
    List<EstadisticaDTO> obtenerEstadisticasPorCategoria();

    // =====================================================
    // ✅ VALIDACIONES Y UTILIDADES
    // =====================================================

    /**
     * Verificar si existe orden
     * GET /ordenes/{id}/existe
     */
    boolean existeOrden(Long ordenId);

    /**
     * Verificar si existe orden por número
     * GET /ordenes/numero/{numeroOrden}/existe
     */
    boolean existeOrdenPorNumero(String numeroOrden);

    /**
     * Validar si se puede crear orden (evitar duplicados)
     */
    boolean puedeCrearOrden(String tipoOrigen, Long origenId, String tipoOrden);

    /**
     * Generar número único de orden
     */
    String generarNumeroOrden();

    // =====================================================
    // 📋 DTOs PARA ESTADÍSTICAS
    // =====================================================

    /**
     * DTO para estadísticas simples
     */
    interface EstadisticaDTO {
        String getCategoria();
        Long getTotal();
        String getDescripcion();
    }

    /**
     * DTO para estadísticas de exámenes
     */
    interface ExamenEstadisticaDTO {
        String getCodigoExamen();
        String getNombreExamen();
        Long getTotalSolicitado();
        String getCategoria();
    }
}