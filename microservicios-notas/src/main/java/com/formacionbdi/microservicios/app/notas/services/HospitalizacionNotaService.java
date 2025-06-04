package com.formacionbdi.microservicios.app.notas.services;

import com.formacionbdi.microservicios.app.notas.models.dto.HospitalizacionNotaDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface del servicio para gestión de notas de hospitalización
 * Define todos los métodos de negocio para el manejo de notas vitales
 */
public interface HospitalizacionNotaService {

    // ===== 🔒 VALIDACIONES CRÍTICAS =====

    /**
     * Verifica si un médico puede crear una nueva nota para una hospitalización
     * Regla: Solo una nota en borrador por médico por hospitalización
     *
     * @param medicoId ID del médico
     * @param hospitalizacionId ID de la hospitalización
     * @return true si puede crear, false si ya tiene una nota en borrador
     */
    boolean puedeCrearNota(Long medicoId, Long hospitalizacionId);

    /**
     * Valida que una nota puede ser modificada
     * Regla: Solo notas en borrador pueden ser modificadas
     *
     * @param notaId ID de la nota
     * @param medicoId ID del médico que intenta modificar
     * @throws NotaBusinessException si no puede ser modificada
     */
    void validarNotaPuedeSerModificada(Long notaId, Long medicoId);

    /**
     * Valida permisos del médico sobre la nota
     *
     * @param notaId ID de la nota
     * @param medicoId ID del médico
     * @throws NotaBusinessException si no tiene permisos
     */
    void validarPermisosMedico(Long notaId, Long medicoId);

    // ===== 📖 CONSULTAS PRINCIPALES =====

    /**
     * Obtiene todas las notas de una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return Lista de notas ordenadas por fecha descendente
     */
    List<HospitalizacionNotaDTO> obtenerNotasPorHospitalizacion(Long hospitalizacionId);

    /**
     * Obtiene una nota específica por ID
     *
     * @param notaId ID de la nota
     * @return Optional con la nota si existe
     */
    Optional<HospitalizacionNotaDTO> obtenerNotaPorId(Long notaId);

    /**
     * Obtiene notas finalizadas de una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return Lista de notas finalizadas
     */
    List<HospitalizacionNotaDTO> obtenerNotasFinalizadas(Long hospitalizacionId);

    /**
     * Obtiene notas en borrador de una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return Lista de notas en borrador
     */
    List<HospitalizacionNotaDTO> obtenerNotasBorrador(Long hospitalizacionId);

    /**
     * Busca notas por número de cuenta
     *
     * @param numeroCuenta Número de cuenta del paciente
     * @return Lista de notas asociadas a la cuenta
     */
    List<HospitalizacionNotaDTO> buscarPorNumeroCuenta(String numeroCuenta);

    // ===== 📝 OPERACIONES CRUD INTELIGENTES =====

    /**
     * Crea una nueva nota de hospitalización
     * Incluye auto-limpieza de audio anterior si es necesario
     *
     * @param notaDTO Datos de la nota a crear
     * @return Nota creada
     * @throws NotaBusinessException si no puede crear (ya tiene borrador)
     */
    HospitalizacionNotaDTO crearNota(HospitalizacionNotaDTO notaDTO);

    /**
     * Actualiza una nota existente
     * Solo permite actualizar notas en borrador
     *
     * @param notaId ID de la nota a actualizar
     * @param notaDTO Nuevos datos de la nota
     * @param medicoId ID del médico que actualiza
     * @return Nota actualizada
     * @throws NotaBusinessException si la nota está finalizada o no tiene permisos
     */
    HospitalizacionNotaDTO actualizarNota(Long notaId, HospitalizacionNotaDTO notaDTO, Long medicoId);

    /**
     * Finaliza una nota (cambia estado de borrador a finalizada)
     * Valida que tenga firma digital si es requerida
     *
     * @param notaId ID de la nota a finalizar
     * @param medicoId ID del médico que finaliza
     * @return Nota finalizada
     * @throws NotaBusinessException si no puede ser finalizada
     */
    HospitalizacionNotaDTO finalizarNota(Long notaId, Long medicoId);

    /**
     * Elimina una nota
     * Solo permite eliminar notas en borrador
     *
     * @param notaId ID de la nota a eliminar
     * @param medicoId ID del médico que elimina
     * @throws NotaBusinessException si la nota está finalizada o no tiene permisos
     */
    void eliminarNota(Long notaId, Long medicoId);

    // ===== 🎯 OPERACIONES ESPECÍFICAS =====

    /**
     * Busca notas por tipo específico en una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @param tipoNota Tipo de nota (01=Evolución, 02=Interconsulta)
     * @return Lista de notas del tipo especificado
     */
    List<HospitalizacionNotaDTO> buscarPorTipo(Long hospitalizacionId, String tipoNota);

    /**
     * Busca notas de un médico en un rango de fechas
     *
     * @param medicoId ID del médico
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de notas en el rango
     */
    List<HospitalizacionNotaDTO> buscarPorMedicoYFechas(Long medicoId,
                                                        LocalDateTime fechaInicio,
                                                        LocalDateTime fechaFin);

    // ===== 🧹 GESTIÓN DE AUDIO Y LIMPIEZA =====

    /**
     * Limpia automáticamente el audio de notas antiguas finalizadas
     * Marca el audio como eliminado sin borrar la transcripción
     *
     * @param diasAntiguedad Días de antigüedad para considerar limpieza
     * @return Número de notas procesadas
     */
    int limpiarAudioAntiguoAutomatico(int diasAntiguedad);

    /**
     * Elimina manualmente el audio de una nota específica
     * Conserva la transcripción si existe
     *
     * @param notaId ID de la nota
     * @param medicoId ID del médico que solicita la eliminación
     * @return true si se eliminó, false si no tenía audio
     */
    boolean eliminarAudioNota(Long notaId, Long medicoId);

    /**
     * Verifica si una nota tiene audio disponible
     *
     * @param notaId ID de la nota
     * @return true si tiene audio no eliminado
     */
    boolean tieneAudioDisponible(Long notaId);

    // ===== 📊 ESTADÍSTICAS Y REPORTES =====

    /**
     * Obtiene estadísticas de notas por hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return Map con estadísticas (total, borradores, finalizadas, con_firma, etc.)
     */
    Map<String, Object> obtenerEstadisticasHospitalizacion(Long hospitalizacionId);

    /**
     * Cuenta notas por estado en una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return Map con contadores por estado
     */
    Map<String, Long> contarNotasPorEstado(Long hospitalizacionId);

    /**
     * Verifica si existe alguna nota para una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return true si existe al menos una nota
     */
    boolean existenNotasParaHospitalizacion(Long hospitalizacionId);

    // ===== 📄 GENERACIÓN DE DOCUMENTOS =====

    /**
     * Genera PDF de una nota específica
     *
     * @param notaId ID de la nota
     * @return Array de bytes del PDF generado
     * @throws NotaProcessingException si no puede generar el PDF
     */
    byte[] generarPdfNota(Long notaId);

    /**
     * Genera PDF consolidado de todas las notas de una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return Array de bytes del PDF consolidado
     * @throws NotaProcessingException si no puede generar el PDF
     */
    byte[] generarPdfConsolidado(Long hospitalizacionId);

    // ===== 🔧 UTILIDADES =====

    /**
     * Valida que los datos JSON sean correctos en una nota
     *
     * @param notaDTO DTO con los datos a validar
     * @throws NotaValidationException si hay errores en los JSON
     */
    void validarDatosJson(HospitalizacionNotaDTO notaDTO);

    /**
     * Obtiene el siguiente número de nota para una hospitalización
     *
     * @param hospitalizacionId ID de la hospitalización
     * @return Número de nota generado automáticamente
     */
    String generarNumeroNota(Long hospitalizacionId);
}