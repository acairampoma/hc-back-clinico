package com.formacionbdi.microservicios.app.receta.services;

import com.formacionbdi.microservicios.app.receta.models.dto.RecetaCabDTO;
import com.formacionbdi.microservicios.app.receta.models.dto.RecetaCompletaDTO;
import com.formacionbdi.microservicios.app.receta.models.dto.MedicamentoVademecumDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 📋 Interface del servicio para gestión de recetas médicas
 * Define todos los métodos de negocio según nuestros 8 endpoints
 */
public interface RecetaService {

    // ===== 📖 CONSULTAS PRINCIPALES =====

    /**
     * Obtiene recetas por tipo de origen y origen ID
     * GET /recetas?tipo_origen=HOS&origen_id=123
     *
     * @param tipoOrigen Tipo de origen (ACT/HOS)
     * @param origenId ID del origen
     * @return Lista de recetas completas
     */
    List<RecetaCompletaDTO> obtenerRecetasPorOrigen(String tipoOrigen, Long origenId);

    /**
     * Obtiene receta específica por ID
     * GET /recetas/{recetaId}
     *
     * @param recetaId ID de la receta
     * @return Optional con la receta completa si existe
     */
    Optional<RecetaCompletaDTO> obtenerRecetaPorId(Long recetaId);

    /**
     * Obtiene receta por número de receta
     * GET /recetas/numero/{numeroReceta}
     *
     * @param numeroReceta Número único de la receta
     * @return Optional con la receta completa si existe
     */
    Optional<RecetaCompletaDTO> obtenerRecetaPorNumero(String numeroReceta);

    /**
     * Obtiene todas las recetas de un paciente
     * GET /recetas/paciente/{pacienteId}
     *
     * @param pacienteId ID del paciente
     * @return Lista de recetas del paciente
     */
    List<RecetaCompletaDTO> obtenerRecetasPorPaciente(Long pacienteId);

    // ===== 📝 OPERACIONES CRUD =====

    /**
     * Crea una nueva receta con lógica de firma automática
     * POST /recetas/crear
     *
     * Incluye:
     * - Validación de reglas de negocio
     * - Auto-generación de número de receta
     * - Lógica de firma automática
     * - Validación de cantidades ≤ 2
     *
     * @param recetaDTO Datos de la receta a crear
     * @return Receta creada completa
     */
    RecetaCompletaDTO crearReceta(RecetaCabDTO recetaDTO);

    /**
     * Actualiza una receta existente
     * PUT /recetas/{recetaId}
     *
     * Validaciones:
     * - Solo hasta 24h antes de vencimiento
     * - Solo el médico creador
     * - Solo recetas activas
     *
     * @param recetaId ID de la receta a actualizar
     * @param recetaDTO Nuevos datos de la receta
     * @param medicoId ID del médico que actualiza
     * @return Receta actualizada
     */
    RecetaCompletaDTO actualizarReceta(Long recetaId, RecetaCabDTO recetaDTO, Long medicoId);

    /**
     * Cambia el estado de una receta
     * PATCH /recetas/{recetaId}/estado
     *
     * Estados posibles:
     * - 01=Activa → 02=Despachada
     * - 01=Activa → 04=Anulada
     *
     * @param recetaId ID de la receta
     * @param nuevoEstado Nuevo estado de la receta
     * @param medicoId ID del médico que cambia el estado
     * @return Receta con estado actualizado
     */
    RecetaCompletaDTO cambiarEstadoReceta(Long recetaId, String nuevoEstado, Long medicoId);

    // ===== 🔍 BÚSQUEDAS DE VADEMÉCUM =====

    /**
     * Busca medicamentos en el vademécum
     *
     * @param busqueda Texto de búsqueda (nombre, marca, concentración)
     * @param categoria Categoría opcional para filtrar
     * @return Lista de medicamentos que coinciden
     */
    List<MedicamentoVademecumDTO> buscarMedicamentos(String busqueda, String categoria);

    /**
     * Obtiene medicamento específico del vademécum
     *
     * @param medicamentoId ID del medicamento
     * @return Optional con el medicamento si existe
     */
    Optional<MedicamentoVademecumDTO> obtenerMedicamento(Long medicamentoId);

    /**
     * Obtiene medicamento por código
     *
     * @param codigoMedicamento Código único del medicamento
     * @return Optional con el medicamento si existe
     */
    Optional<MedicamentoVademecumDTO> obtenerMedicamentoPorCodigo(String codigoMedicamento);

    /**
     * Obtiene categorías de medicamentos disponibles
     *
     * @return Lista de categorías únicas
     */
    List<String> obtenerCategoriasMedicamentos();

    // ===== 📊 ESTADÍSTICAS Y REPORTES =====

    /**
     * Obtiene estadísticas de recetas por médico
     *
     * @param medicoId ID del médico
     * @return Map con estadísticas (total, activas, despachadas, firmadas)
     */
    Map<String, Object> obtenerEstadisticasMedico(Long medicoId);

    /**
     * Obtiene medicamentos más prescritos
     *
     * @param limite Número máximo de resultados
     * @return Lista de medicamentos ordenados por frecuencia
     */
    List<Map<String, Object>> obtenerMedicamentosMasPrescritos(int limite);

    // ===== 🔒 VALIDACIONES =====

    /**
     * Verifica si se puede crear una receta para el origen especificado
     *
     * @param tipoOrigen Tipo de origen (ACT/HOS)
     * @param origenId ID del origen
     * @return true si se puede crear, false si ya existe una para hoy
     */
    boolean puedeCrearReceta(String tipoOrigen, Long origenId);

    /**
     * Verifica si una receta puede ser modificada
     *
     * @param recetaId ID de la receta
     * @param medicoId ID del médico
     * @return true si puede ser modificada
     */
    boolean puedeModificarReceta(Long recetaId, Long medicoId);

    // ===== 🛠️ UTILIDADES =====

    /**
     * Genera el siguiente número de receta disponible
     *
     * @return Número de receta único generado
     */
    String generarNumeroReceta();

    /**
     * Valida que todos los medicamentos de una receta estén disponibles
     *
     * @param recetaDTO Receta a validar
     * @return true si todos los medicamentos están disponibles
     */
    boolean validarDisponibilidadMedicamentos(RecetaCabDTO recetaDTO);

    /**
     * Aplica lógica de firma automática según reglas de negocio
     *
     * @param recetaDTO Receta a evaluar para firma
     * @return RecetaDTO con firma aplicada si cumple condiciones
     */
    RecetaCabDTO aplicarFirmaAutomatica(RecetaCabDTO recetaDTO);
}
