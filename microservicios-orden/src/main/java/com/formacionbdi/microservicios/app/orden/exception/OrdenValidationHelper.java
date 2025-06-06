package com.formacionbdi.microservicios.app.orden.exception;

import com.formacionbdi.microservicios.app.orden.models.entity.OrdenCab;
import com.formacionbdi.microservicios.app.orden.models.entity.OrdenDet;
import com.formacionbdi.microservicios.app.orden.models.dto.OrdenCabDTO;
import com.formacionbdi.microservicios.app.orden.models.dto.ActualizarOrdenDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 🛡️ VALIDATION HELPER - TODA LA LÓGICA DE VALIDACIÓN CENTRALIZADA
 * Service Impl queda súper limpio
 */
@Component
public class OrdenValidationHelper {

    // =====================================================
    // VALIDACIONES DE CREACIÓN
    // =====================================================

    public void validarCreacionOrden(OrdenCabDTO ordenDTO) {
        // Validar campos obligatorios ya están con @NotNull

        // Validar tipo de origen válido
        if (!esTipoOrigenValido(ordenDTO.getTipoOrigen())) {
            throw new OrdenBusinessException("ORDEN_001",
                    "Tipo de origen inválido: " + ordenDTO.getTipoOrigen() + ". Debe ser HOS, AMB o EMR");
        }

        // Validar tipo de orden válido
        if (!esTipoOrdenValido(ordenDTO.getTipoOrden())) {
            throw new OrdenBusinessException("ORDEN_002",
                    "Tipo de orden inválido: " + ordenDTO.getTipoOrden() + ". Debe ser LAB, IMG, PROC o FUNC");
        }

        // Validar prioridad si se proporciona
        if (ordenDTO.getPrioridad() != null && !esPrioridadValida(ordenDTO.getPrioridad())) {
            throw new OrdenBusinessException("ORDEN_003",
                    "Prioridad inválida: " + ordenDTO.getPrioridad() + ". Debe ser E, U o N");
        }

        // Validar fecha programada no sea pasada
        if (ordenDTO.getFechaProgramada() != null && ordenDTO.getFechaProgramada().isBefore(LocalDate.now())) {
            throw new OrdenBusinessException("ORDEN_004",
                    "La fecha programada no puede ser anterior a hoy");
        }

        // Validar que tenga al menos un examen
        if (ordenDTO.getExamenes() == null || ordenDTO.getExamenes().isEmpty()) {
            throw new OrdenBusinessException("ORDEN_005",
                    "La orden debe tener al menos un examen");
        }

        // Validar máximo de exámenes por orden (opcional)
        if (ordenDTO.getExamenes().size() > 10) {
            throw new OrdenBusinessException("ORDEN_006",
                    "Máximo 10 exámenes por orden. Cantidad actual: " + ordenDTO.getExamenes().size());
        }
    }

    // =====================================================
    // VALIDACIONES DE ACTUALIZACIÓN
    // =====================================================

    public void validarActualizacionOrden(OrdenCab ordenExistente, ActualizarOrdenDTO actualizarDTO) {
        // Validar que la orden pueda ser modificada
        if (!ordenExistente.puedeSerModificada()) {
            throw new OrdenBusinessException("ORDEN_007",
                    "La orden no puede ser modificada. Estado actual: " + ordenExistente.getEstadoDescripcion());
        }

        // Validar prioridad si se actualiza
        if (actualizarDTO.getPrioridad() != null && !esPrioridadValida(actualizarDTO.getPrioridad())) {
            throw new OrdenBusinessException("ORDEN_008",
                    "Prioridad inválida: " + actualizarDTO.getPrioridad());
        }

        // Validar fecha programada
        if (actualizarDTO.getFechaProgramada() != null &&
                actualizarDTO.getFechaProgramada().isBefore(LocalDate.now())) {
            throw new OrdenBusinessException("ORDEN_009",
                    "La fecha programada no puede ser anterior a hoy");
        }
    }

    // =====================================================
    // VALIDACIONES DE ESTADO
    // =====================================================

    public void validarCambioEstado(OrdenCab orden, String nuevoEstado) {
        String estadoActual = orden.getEstado();

        // Validar estado válido
        if (!esEstadoValido(nuevoEstado)) {
            throw new OrdenBusinessException("ORDEN_010",
                    "Estado inválido: " + nuevoEstado);
        }

        // Validar transiciones de estado
        if (!esTransicionValida(estadoActual, nuevoEstado)) {
            throw new OrdenBusinessException("ORDEN_011",
                    "Transición de estado inválida: " + estadoActual + " → " + nuevoEstado);
        }

        // No permitir cambios en órdenes completadas o canceladas
        if ("04".equals(estadoActual) || "05".equals(estadoActual)) {
            throw new OrdenBusinessException("ORDEN_012",
                    "No se puede cambiar el estado de una orden completada o cancelada");
        }
    }

    // =====================================================
    // VALIDACIONES DE EXÁMENES
    // =====================================================

    public void validarExamenes(List<Long> examenesIds) {
        if (examenesIds == null || examenesIds.isEmpty()) {
            throw new OrdenBusinessException("ORDEN_013",
                    "Debe especificar al menos un examen");
        }

        // Validar duplicados
        long examenesUnicos = examenesIds.stream().distinct().count();
        if (examenesUnicos != examenesIds.size()) {
            throw new OrdenBusinessException("ORDEN_014",
                    "No se permiten exámenes duplicados en la misma orden");
        }
    }

    // =====================================================
    // VALIDACIONES DE DUPLICADOS
    // =====================================================

    public void validarOrdenNoDuplicada(String tipoOrigen, Long origenId, String tipoOrden, boolean existe) {
        if (existe) {
            throw new OrdenBusinessException("ORDEN_015",
                    String.format("Ya existe una orden de tipo %s para %s %d en el día de hoy",
                            tipoOrden, tipoOrigen, origenId));
        }
    }

    // =====================================================
    // MÉTODOS HELPER PRIVADOS
    // =====================================================

    private boolean esTipoOrigenValido(String tipoOrigen) {
        return "HOS".equals(tipoOrigen) || "AMB".equals(tipoOrigen) || "EMR".equals(tipoOrigen);
    }

    private boolean esTipoOrdenValido(String tipoOrden) {
        return "LAB".equals(tipoOrden) || "IMG".equals(tipoOrden) ||
                "PROC".equals(tipoOrden) || "FUNC".equals(tipoOrden);
    }

    private boolean esPrioridadValida(String prioridad) {
        return "E".equals(prioridad) || "U".equals(prioridad) || "N".equals(prioridad);
    }

    private boolean esEstadoValido(String estado) {
        return "01".equals(estado) || "02".equals(estado) || "03".equals(estado) ||
                "04".equals(estado) || "05".equals(estado);
    }

    private boolean esTransicionValida(String estadoActual, String nuevoEstado) {
        switch (estadoActual) {
            case "01": // Solicitada → Programada, En Proceso, Cancelada
                return "02".equals(nuevoEstado) || "03".equals(nuevoEstado) || "05".equals(nuevoEstado);
            case "02": // Programada → En Proceso, Completada, Cancelada
                return "03".equals(nuevoEstado) || "04".equals(nuevoEstado) || "05".equals(nuevoEstado);
            case "03": // En Proceso → Completada, Cancelada
                return "04".equals(nuevoEstado) || "05".equals(nuevoEstado);
            case "04": // Completada → No cambios
            case "05": // Cancelada → No cambios
                return false;
            default:
                return false;
        }
    }
}