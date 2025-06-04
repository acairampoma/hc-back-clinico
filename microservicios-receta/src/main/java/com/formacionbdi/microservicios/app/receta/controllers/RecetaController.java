package com.formacionbdi.microservicios.app.receta.controllers;

import com.formacionbdi.microservicios.app.receta.models.dto.*;
import com.formacionbdi.microservicios.app.receta.models.response.ApiResponse;
import com.formacionbdi.microservicios.app.receta.services.RecetaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 📋 CONTROLLER para gestión de recetas médicas
 * Aplicando programación funcional y nuestro patrón exitoso
 *
 * 🏆 TÉCNICA USADA EN NOTAS Y RECETAS:
 * - Entity → DTO → Repository → Exception → Service → Service Impl → Controller
 * - Exception Layer maneja TODO (validaciones, reglas de negocio)
 * - Service Impl súper limpio (programación funcional)
 * - Controller con helper funcional para manejo de respuestas
 * - JOIN inteligente para performance (evita llamadas entre microservicios)
 * - Patrón CAB+DET transaccional
 */
@RestController
@RequestMapping("/recetas")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RecetaController {

    private final RecetaService recetaService;

    // ===== 📖 CONSULTAS PRINCIPALES =====

    /**
     * Obtiene recetas por tipo de origen y origen ID
     * GET /recetas?tipo_origen=HOS&origen_id=123
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecetaCompletaDTO>>> obtenerRecetasPorOrigen(
            @RequestParam String tipo_origen,
            @RequestParam @NotNull @Min(1) Long origen_id) {

        log.info("📖 Obteniendo recetas para {} ID {}", tipo_origen, origen_id);

        return ejecutarConManejo(() -> {
            var recetas = recetaService.obtenerRecetasPorOrigen(tipo_origen, origen_id);
            return ApiResponse.success(recetas,
                    String.format("Recetas obtenidas para %s ID %d (%d encontradas)",
                            tipo_origen, origen_id, recetas.size()));
        });
    }

    /**
     * Obtiene receta específica por ID
     * GET /recetas/{recetaId}
     */
    @GetMapping("/{recetaId}")
    public ResponseEntity<ApiResponse<RecetaCompletaDTO>> obtenerRecetaPorId(
            @PathVariable @NotNull @Min(1) Long recetaId) {

        log.info("📖 Obteniendo receta ID {}", recetaId);

        return ejecutarConManejo(() ->
                recetaService.obtenerRecetaPorId(recetaId)
                        .map(receta -> ApiResponse.success(receta, "Receta obtenida exitosamente"))
                        .orElse(ApiResponse.error("Receta no encontrada"))
        );
    }

    /**
     * Obtiene receta por número de receta
     * GET /recetas/numero/{numeroReceta}
     */
    @GetMapping("/numero/{numeroReceta}")
    public ResponseEntity<ApiResponse<RecetaCompletaDTO>> obtenerRecetaPorNumero(
            @PathVariable String numeroReceta) {

        log.info("📖 Obteniendo receta número {}", numeroReceta);

        return ejecutarConManejo(() ->
                recetaService.obtenerRecetaPorNumero(numeroReceta)
                        .map(receta -> ApiResponse.success(receta,
                                String.format("Receta %s obtenida exitosamente", numeroReceta)))
                        .orElse(ApiResponse.error("Receta no encontrada"))
        );
    }

    /**
     * Obtiene todas las recetas de un paciente
     * GET /recetas/paciente/{pacienteId}
     */
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<ApiResponse<List<RecetaCompletaDTO>>> obtenerRecetasPorPaciente(
            @PathVariable @NotNull @Min(1) Long pacienteId) {

        log.info("📖 Obteniendo recetas del paciente {}", pacienteId);

        return ejecutarConManejo(() -> {
            var recetas = recetaService.obtenerRecetasPorPaciente(pacienteId);
            return ApiResponse.success(recetas,
                    String.format("Recetas del paciente %d (%d encontradas)",
                            pacienteId, recetas.size()));
        });
    }

    // ===== 📝 OPERACIONES CRUD =====

    /**
     * Crea una nueva receta con lógica de firma automática
     * POST /recetas/crear
     *
     * 🔥 INCLUYE:
     * - Validación regla: No receta duplicada mismo día
     * - Validación cantidad: ≤ 2 unidades por medicamento
     * - Auto-generación número de receta
     * - Lógica de firma automática
     * - Transacción CAB+DET
     */
    @PostMapping("/crear")
    public ResponseEntity<ApiResponse<RecetaCompletaDTO>> crearReceta(
            @Valid @RequestBody RecetaCabDTO recetaDTO) {

        log.info("📝 Creando receta para {} ID {} por médico {}",
                recetaDTO.getTipoOrigen(), recetaDTO.getOrigenId(), recetaDTO.getCreadoPor());

        return ejecutarConManejo(() -> {
            var recetaCreada = recetaService.crearReceta(recetaDTO);
            return ApiResponse.success(recetaCreada,
                    String.format("Receta creada exitosamente: %s", recetaCreada.getRecetaInfo().getNumeroReceta()));
        }, HttpStatus.CREATED);
    }

    /**
     * Actualiza una receta existente
     * PUT /recetas/{recetaId}
     *
     * 🔒 VALIDACIONES:
     * - Solo hasta 24h antes de vencimiento
     * - Solo el médico creador
     * - Solo recetas activas
     */
    @PutMapping("/{recetaId}")
    public ResponseEntity<ApiResponse<RecetaCompletaDTO>> actualizarReceta(
            @PathVariable Long recetaId,
            @Valid @RequestBody ActualizarRecetaDTO actualizarDTO,
            @RequestParam Long medicoId) {

        log.info("🔄 Actualizando receta {} por médico {}", recetaId, medicoId);

        return ejecutarConManejo(() -> {
            // Convertir ActualizarRecetaDTO a RecetaCabDTO
            RecetaCabDTO recetaDTO = RecetaCabDTO.builder()
                    .diagnosticoPrincipal(actualizarDTO.getDiagnosticoPrincipal())
                    .indicacionesGenerales(actualizarDTO.getIndicacionesGenerales())
                    .fechaVencimiento(actualizarDTO.getFechaVencimiento())
                    .build();

            // Usar tu método existente
            RecetaCompletaDTO resultado = recetaService.actualizarReceta(recetaId, recetaDTO, medicoId);

            return ApiResponse.success(resultado, "Receta actualizada exitosamente");
        });
    }

    /**
     * Cambia el estado de una receta
     * PATCH /recetas/{recetaId}/estado
     *
     * Estados: 01=Activa → 02=Despachada, 04=Anulada
     */
    @PatchMapping("/{recetaId}/estado")
    public ResponseEntity<ApiResponse<RecetaCompletaDTO>> cambiarEstadoReceta(
            @PathVariable @NotNull @Min(1) Long recetaId,
            @RequestParam String nuevo_estado,
            @RequestParam @NotNull @Min(1) Long medicoId) {

        log.info("📊 Cambiando estado de receta {} a {} por médico {}",
                recetaId, nuevo_estado, medicoId);

        return ejecutarConManejo(() -> {
            var recetaActualizada = recetaService.cambiarEstadoReceta(recetaId, nuevo_estado, medicoId);
            String estadoDescripcion = obtenerDescripcionEstado(nuevo_estado);
            return ApiResponse.success(recetaActualizada,
                    String.format("Estado cambiado a: %s", estadoDescripcion));
        });
    }

    // ===== 🔍 BÚSQUEDAS DE VADEMÉCUM =====

    /**
     * Busca medicamentos en el vademécum
     * GET /recetas/medicamentos/buscar?q=paracetamol&categoria=analgesico
     */
    @GetMapping("/medicamentos/buscar")
    public ResponseEntity<ApiResponse<List<MedicamentoVademecumDTO>>> buscarMedicamentos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria) {

        log.info("🔍 Buscando medicamentos: '{}', categoría: '{}'", q, categoria);

        return ejecutarConManejo(() -> {
            var medicamentos = recetaService.buscarMedicamentos(q, categoria);
            return ApiResponse.success(medicamentos,
                    String.format("Medicamentos encontrados (%d resultados)", medicamentos.size()));
        });
    }

    /**
     * Obtiene medicamento específico del vademécum
     * GET /recetas/medicamentos/{medicamentoId}
     */
    @GetMapping("/medicamentos/{medicamentoId}")
    public ResponseEntity<ApiResponse<MedicamentoVademecumDTO>> obtenerMedicamento(
            @PathVariable @NotNull @Min(1) Long medicamentoId) {

        return ejecutarConManejo(() ->
                recetaService.obtenerMedicamento(medicamentoId)
                        .map(medicamento -> ApiResponse.success(medicamento, "Medicamento obtenido exitosamente"))
                        .orElse(ApiResponse.error("Medicamento no encontrado"))
        );
    }

    /**
     * Obtiene categorías de medicamentos
     * GET /recetas/medicamentos/categorias
     */
    @GetMapping("/medicamentos/categorias")
    public ResponseEntity<ApiResponse<List<String>>> obtenerCategoriasMedicamentos() {
        return ejecutarConManejo(() -> {
            var categorias = recetaService.obtenerCategoriasMedicamentos();
            return ApiResponse.success(categorias,
                    String.format("Categorías obtenidas (%d disponibles)", categorias.size()));
        });
    }

    // ===== 📊 ESTADÍSTICAS =====

    /**
     * Obtiene estadísticas de recetas por médico
     * GET /recetas/estadisticas/medico/{medicoId}
     */
    @GetMapping("/estadisticas/medico/{medicoId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadisticasMedico(
            @PathVariable @NotNull @Min(1) Long medicoId) {

        log.info("📊 Obteniendo estadísticas del médico {}", medicoId);

        return ejecutarConManejo(() -> {
            var estadisticas = recetaService.obtenerEstadisticasMedico(medicoId);
            return ApiResponse.success(estadisticas, "Estadísticas obtenidas exitosamente");
        });
    }

    /**
     * Obtiene medicamentos más prescritos
     * GET /recetas/estadisticas/medicamentos-mas-prescritos?limite=10
     */
    @GetMapping("/estadisticas/medicamentos-mas-prescritos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerMedicamentosMasPrescritos(
            @RequestParam(defaultValue = "10") int limite) {

        return ejecutarConManejo(() -> {
            var medicamentos = recetaService.obtenerMedicamentosMasPrescritos(limite);
            return ApiResponse.success(medicamentos,
                    String.format("Top %d medicamentos más prescritos", limite));
        });
    }

    // ===== 🛠️ UTILIDADES =====

    /**
     * Health Check del microservicio
     * GET /recetas/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "microservicio", "recetas-medicas",
                "version", "1.0.0",
                "puerto", 8005,
                "timestamp", LocalDateTime.now(),
                "endpoints_disponibles", List.of(
                        "GET /recetas?tipo_origen={tipo}&origen_id={id}",
                        "GET /recetas/{recetaId}",
                        "GET /recetas/numero/{numeroReceta}",
                        "GET /recetas/paciente/{pacienteId}",
                        "POST /recetas/crear",
                        "PUT /recetas/{recetaId}",
                        "PATCH /recetas/{recetaId}/estado",
                        "GET /recetas/medicamentos/buscar"
                ),
                "reglas_negocio", List.of(
                        "🔒 No receta duplicada mismo día",
                        "⏰ Modificación hasta 24h antes vencimiento",
                        "💊 Cantidad máxima 2 unidades por medicamento",
                        "✍️ Firma automática según condiciones",
                        "🔗 JOIN inteligente con vademécum"
                )
        );

        return ResponseEntity.ok(ApiResponse.success(health, "Microservicio de recetas operativo"));
    }

    // ===== 🔧 HELPER FUNCIONAL =====

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
            log.error("❌ Error en operación de receta: {}", e.getMessage(), e);
            throw e; // Re-lanzar para que lo maneje GlobalExceptionHandler
        }
    }

    private String obtenerDescripcionEstado(String estado) {
        Map<String, String> descripciones = Map.of(
                "01", "Activa",
                "02", "Despachada",
                "03", "Vencida",
                "04", "Anulada"
        );
        return descripciones.getOrDefault(estado, "Desconocido");
    }
}