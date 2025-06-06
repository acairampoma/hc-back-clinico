package com.formacionbdi.microservicios.app.catalogos.controllers;

import com.formacionbdi.microservicios.app.catalogos.models.dto.CatalogosDTO;
import com.formacionbdi.microservicios.app.catalogos.models.response.ApiResponse;
import com.formacionbdi.microservicios.app.catalogos.services.CatalogosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🔍 Controller REST para gestión de catálogos
 * Puerto: 8009 - Microservicio Catálogos
 */
@RestController
@RequestMapping("/catalogos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CatalogosController {

    private final CatalogosService catalogosService;

    // ===== BÚSQUEDAS GENERALES =====

    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> buscarPorTermino(
            @RequestParam String q) {
        log.info("🔍 REST - Búsqueda general: {}", q);

        List<CatalogosDTO> resultados = catalogosService.buscarPorTermino(q);
        return ResponseEntity.ok(ApiResponse.success(resultados, "Búsqueda completada"));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponse<CatalogosDTO>> buscarPorCodigo(@PathVariable String codigo) {
        log.info("🎯 REST - Búsqueda por código: {}", codigo);

        return catalogosService.buscarPorCodigoExacto(codigo)
                .map(resultado -> ResponseEntity.ok(ApiResponse.success(resultado, "Código encontrado")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Código no encontrado: " + codigo)));
    }

    // ===== BÚSQUEDAS POR TIPO =====

    @GetMapping("/examenes/buscar")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> buscarExamenes(@RequestParam String q) {
        log.info("🔬 REST - Búsqueda exámenes: {}", q);

        List<CatalogosDTO> examenes = catalogosService.buscarExamenes(q);
        return ResponseEntity.ok(ApiResponse.success(examenes, "Exámenes encontrados"));
    }

    @GetMapping("/medicamentos/buscar")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> buscarMedicamentos(@RequestParam String q) {
        log.info("💊 REST - Búsqueda medicamentos: {}", q);

        List<CatalogosDTO> medicamentos = catalogosService.buscarMedicamentos(q);
        return ResponseEntity.ok(ApiResponse.success(medicamentos, "Medicamentos encontrados"));
    }

    @GetMapping("/maestros/buscar")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> buscarCatalogos(@RequestParam String q) {
        log.info("📊 REST - Búsqueda catálogos: {}", q);

        List<CatalogosDTO> catalogos = catalogosService.buscarCatalogos(q);
        return ResponseEntity.ok(ApiResponse.success(catalogos, "Catálogos encontrados"));
    }

    // ===== BÚSQUEDAS POR CATEGORÍA =====

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> buscarPorCategoria(@PathVariable String categoria) {
        log.info("🏷️ REST - Búsqueda por categoría: {}", categoria);

        List<CatalogosDTO> resultados = catalogosService.buscarPorCategoria(categoria);
        return ResponseEntity.ok(ApiResponse.success(resultados, "Categoría consultada"));
    }

    @GetMapping("/tabla/{tablaCodigo}")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> buscarEnTabla(@PathVariable String tablaCodigo) {
        log.info("📋 REST - Búsqueda en tabla: {}", tablaCodigo);

        List<CatalogosDTO> resultados = catalogosService.buscarEnTablaEspecifica(tablaCodigo);
        return ResponseEntity.ok(ApiResponse.success(resultados, "Tabla consultada"));
    }

    // ===== LISTADOS COMPLETOS =====

    @GetMapping("/examenes")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> obtenerTodosLosExamenes() {
        log.info("📋 REST - Todos los exámenes");

        List<CatalogosDTO> examenes = catalogosService.obtenerTodosLosExamenes();
        return ResponseEntity.ok(ApiResponse.success(examenes, "Exámenes obtenidos"));
    }

    @GetMapping("/medicamentos")
    public ResponseEntity<ApiResponse<List<CatalogosDTO>>> obtenerTodosLosMedicamentos() {
        log.info("💊 REST - Todos los medicamentos");

        List<CatalogosDTO> medicamentos = catalogosService.obtenerTodosLosMedicamentos();
        return ResponseEntity.ok(ApiResponse.success(medicamentos, "Medicamentos obtenidos"));
    }

    @GetMapping("/categorias")
    public ResponseEntity<ApiResponse<List<String>>> obtenerCategorias() {
        log.info("🏷️ REST - Todas las categorías");

        List<String> categorias = catalogosService.obtenerTodasLasCategorias();
        return ResponseEntity.ok(ApiResponse.success(categorias, "Categorías obtenidas"));
    }

    @GetMapping("/tipos")
    public ResponseEntity<ApiResponse<List<String>>> obtenerTipos() {
        log.info("📊 REST - Tipos de tabla");

        List<String> tipos = catalogosService.obtenerTiposDeTabla();
        return ResponseEntity.ok(ApiResponse.success(tipos, "Tipos obtenidos"));
    }

    // ===== UTILIDADES =====

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        log.info("💚 REST - Health check catálogos");

        return ResponseEntity.ok(ApiResponse.success(
                "Puerto: 8009 - Version: 1.0.0",
                "Microservicio catálogos funcionando correctamente"));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<String[]>> infoMicroservicio() {
        log.info("ℹ️ REST - Info catálogos");

        String[] info = {
                "🔍 Microservicio Catálogos y Búsquedas",
                "📋 Puerto: 8009",
                "🔬 Funcionalidad: Búsqueda unificada",
                "💊 Examenes y medicamentos",
                "📊 Catálogos administrativos",
                "🎯 Búsqueda por código exacto",
                "🏷️ Filtros por categoría",
                "⚡ Performance optimizada con vista JSON"
        };

        return ResponseEntity.ok(ApiResponse.success(info, "Información del microservicio"));
    }
}