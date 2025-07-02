package com.formacionbdi.microservicios.app.receta.exception;

import com.formacionbdi.microservicios.app.receta.models.dto.RecetaCabDTO;
import com.formacionbdi.microservicios.app.receta.models.dto.RecetaDetDTO;
import com.formacionbdi.microservicios.app.receta.models.entity.RecetaCab;
import com.formacionbdi.microservicios.app.receta.repository.RecetaCabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Helper para todas las validaciones de recetas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecetaValidationHelper {

    private final RecetaCabRepository recetaCabRepository;

    // ===== VALIDACIONES DE CREACIÓN =====

    /**
     * Valida que se pueda crear una nueva receta
     */
    public void validarCreacionReceta(RecetaCabDTO dto) {
        log.debug("Validando creación de receta para {} ID {}", dto.getTipoOrigen(), dto.getOrigenId());

        // Regla 1: No receta duplicada mismo día
        validarRecetaDuplicadaMismoDia(dto.getTipoOrigen(), dto.getOrigenId());

        // Regla 2: Validar medicamentos
        validarMedicamentosReceta(dto.getMedicamentos());

        // Regla 3: Validar fechas
        validarFechasReceta(dto);

        // Regla 4: Validar cantidades
        validarCantidadesMedicamentos(dto.getMedicamentos());
    }

    /**
     * Valida que se pueda actualizar una receta
     */
    public void validarActualizacionReceta(Long recetaId, Long medicoId) {
        log.debug("Validando actualización de receta {} por médico {}", recetaId, medicoId);

        RecetaCab receta = recetaCabRepository.findById(recetaId)
                .orElseThrow(() -> RecetaNotFoundException.receta(recetaId));

        // Regla 1: Solo hasta 24h antes de vencimiento
        validarTiempoParaActualizar(receta);

        // Regla 2: Solo el médico creador
        validarPermisosMedico(receta, medicoId);

        // Regla 3: Solo recetas activas
        validarEstadoModificable(receta);
    }

    // ===== VALIDACIONES DE TIEMPO =====

    private void validarTiempoParaActualizar(RecetaCab receta) {
        if (receta.getFechaVencimiento() == null) {
            throw new RecetaValidationException("La fecha de vencimiento es inválida");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = receta.getFechaVencimiento().atStartOfDay().minusHours(24);

        if (ahora.isAfter(limite)) {
            throw new RecetaBusinessException("La receta " + receta.getId() + " no puede ser modificada");
        }
    }

    // ===== VALIDACIONES DE PERMISOS =====

    private void validarPermisosMedico(RecetaCab receta, Long medicoId) {
        if (!receta.getMedicoId().equals(medicoId)) {
            throw RecetaBusinessException.permisosDenegados(medicoId, receta.getId());
        }
    }

    // ===== VALIDACIONES DE ESTADO =====

    private void validarEstadoModificable(RecetaCab receta) {
        if (!"01".equals(receta.getEstado())) {
            throw RecetaBusinessException.conflictoEstado(
                String.format("La receta %d ya está finalizada", receta.getId())
            );
        }
    }

    // ===== VALIDACIONES DE DUPLICACIÓN =====

    private void validarRecetaDuplicadaMismoDia(String tipoOrigen, Long origenId) {
        // NUEVA LÓGICA: Solo validar medicamentos duplicados, NO recetas duplicadas
        log.debug("Permitiendo múltiples recetas del mismo origen en el mismo día");
    }

    // ===== VALIDACIONES DE MEDICAMENTOS =====

    private void validarMedicamentosReceta(List<RecetaDetDTO> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            throw RecetaBusinessException.datosInvalidos("No se han especificado medicamentos");
        }

        for (RecetaDetDTO medicamento : medicamentos) {
            validarMedicamentoIndividual(medicamento);
        }
    }

    private void validarMedicamentoIndividual(RecetaDetDTO medicamento) {
        // Validar dosis
        if (medicamento.getDosis() == null || medicamento.getDosis().trim().isEmpty()) {
            throw RecetaBusinessException.datosInvalidos("La dosis es inválida: " + medicamento.getDosis());
        }

        // Validar frecuencia
        if (medicamento.getFrecuencia() == null || medicamento.getFrecuencia().trim().isEmpty()) {
            throw RecetaBusinessException.datosInvalidos("La frecuencia es inválida: " + medicamento.getFrecuencia());
        }
    }

    // ===== VALIDACIÓN DE MEDICAMENTOS DUPLICADOS =====

    private void validarMedicamentosDuplicadosMismoDia(String tipoOrigen, Long origenId, List<RecetaDetDTO> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return;
        }

        List<Long> medicamentoIds = medicamentos.stream()
                .map(RecetaDetDTO::getMedicamentoId)
                .toList();

        boolean tieneMedicamentosDuplicados = recetaCabRepository.existeRecetaConMedicamentosDuplicadosHoy(
                tipoOrigen, origenId, medicamentoIds
        );

        if (tieneMedicamentosDuplicados) {
            throw RecetaBusinessException.conflictoEstado(
                String.format("Ya existe una receta con los mismos medicamentos hoy para %s %d", 
                    tipoOrigen, origenId)
            );
        }
    }

    // ===== VALIDACIONES DE CANTIDAD =====

    private void validarCantidadesMedicamentos(List<RecetaDetDTO> medicamentos) {
        for (RecetaDetDTO medicamento : medicamentos) {
            validarCantidadMedicamento(medicamento);
        }
    }

    private void validarCantidadMedicamento(RecetaDetDTO medicamento) {
        BigDecimal cantidad = medicamento.getCantidadTotal();
        if (cantidad == null) {
            throw RecetaBusinessException.datosInvalidos("La cantidad es inválida para: cantidad_total = 0");
        }

        if (cantidad.compareTo(BigDecimal.ZERO) <= 0 || cantidad.compareTo(new BigDecimal("2")) > 0) {
            throw RecetaBusinessException.noProcesable(
                String.format("Cantidad excesiva para el medicamento %s: %s", 
                    medicamento.getCodigoMedicamento(), cantidad.toString())
            );
        }
    }

    // ===== VALIDACIONES DE FECHAS =====

    private void validarFechasReceta(RecetaCabDTO dto) {
        if (dto.getFechaVencimiento() != null && dto.getFechaReceta() != null) {
            if (dto.getFechaVencimiento().isBefore(dto.getFechaReceta().toLocalDate())) {
                throw RecetaBusinessException.datosInvalidos("La fecha de vencimiento es inválida");
            }
        }
    }
}